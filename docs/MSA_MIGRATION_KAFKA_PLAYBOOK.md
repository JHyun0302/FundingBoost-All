# FundingBoost MSA 마이그레이션 플레이북 (Kafka + 스키마 분리)

## 0) 현재 원칙
- DB는 **MySQL 인스턴스 1개**를 유지한다.
- 도메인 경계는 **스키마 단위**로 나눈다.
- 서비스 간 상태 전파는 **동기 REST 최소화 + 비동기 이벤트(Kafka)** 로 진행한다.

## 1) 목표 스키마 구조 (단일 MySQL 인스턴스)
- `fundingboost`: Identity/Funding/Commerce/Admin/Outbox
- `item`: Catalog (크롤러/상품 원천 데이터)
- `payment`: Payment Intent/Attempt

참고: MySQL에서는 schema와 database가 동일 개념이다.
즉 "서버 1대 + MySQL 1개 인스턴스" 안에서 논리 스키마(`fundingboost`, `item`, `payment`)만 나눈다.

## 2) 마이그레이션 순서
1. 모놀리식 내부에 경계 도입 (진행 중)
2. Catalog 서비스 분리 (`/api/v1/items`, `/api/v3/*`)
3. Payment 서비스 분리
4. Funding 서비스 분리
5. Commerce 서비스 분리
6. Home/Admin 조회를 Projection(Read Model)로 분리

## 2-1) 현재 진행도 체크 (2026-03-25)
- 1단계 모놀리스 내부 경계 고정: 진행중
  - 완료: Outbox/Kafka, Payment 스키마 분리, Catalog 포트 + 원격 어댑터/플래그
  - 잔여: 일부 내부 경로 직접 조회 의존 정리
- 2단계 Catalog 분리: 완료
  - 완료: Catalog 스켈레톤 API, 북마크/응답 스펙 필수 갭, 원격 경로 준비, 엣지 라우팅/카나리 설정, 5% -> 25% -> 50% -> 100% -> full 전환, Back fallback 제거, 랭킹 고도화, QA 계정(qa1~qa5) 검증
- 3단계 Payment 분리: 완료
  - 완료: payment 스키마 운용, PaymentIntent/Attempt 저장소, Payment facade 도입, payment 프로세스 배포(`:8081`), `/api/v1/pay/**` GET proxy + POST command-proxy 이관, edge `/api/v1/pay` canary 25% -> 50% -> 100% -> full, `payment.completed.v1`/`payment.failed.v1` 계약 정의
  - 추가 완료: `POST /api/v1/pay/order`, `POST /api/v1/pay/order/now`, `POST /api/v1/pay/funding/{fundingItemId}`, `POST /api/v1/pay/friends/*`를 Payment 서비스 native 실행/영속화 경로로 이전, back JWT 재사용용 internal auth bridge 추가, Catalog 분리 이후 funding item 조회를 Catalog reader 기반으로 보정, Payment 내부 `PaymentOrderFinalizeService`로 order/funding finalize 이동, native 오류 응답 header 보정
  - 후속 ownership 완료: Payment가 `commerce.order.paid.v1`, `funding.contribution.created.v1`를 직접 outbox에 적재하고, Back `APP_PAYMENT_FOLLOW_UP_MODE=direct`, `APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED=false`로 전환
  - 검증: QA 계정(`qa1~qa5`) 기준 `view/order`, `view/funding/*`, `friends/*` back/payment 동일 응답 확인, invalid friend write parity 확인, `X-Payment-Service-Mode=native-friend` 오류 응답 확인, native `order/now` 성공 확인, native `order` idempotency 재호출 시 중복 주문/이벤트 미생성 확인, funding remain/direct friend/barcode 성공 확인, 2026-03-25 기준 신규 `commerce.order.paid.v1` / `funding.contribution.created.v1`가 Payment outbox에만 생성되고 Back outbox에는 생성되지 않음을 확인
  - 운영 보정: Kafka 이미지를 `apache/kafka:3.7.2`로 교체, `OutboxRelayService` self-invocation 트랜잭션 버그 수정, pending outbox `PUBLISHED` 확인, `payment.completed.v1`/`commerce.order.paid.v1` dedupe guard 추가, `funding.contribution.created.v1` key를 contribution 단위(`fundingId:contributorId`)로 보정
- 4단계 Funding/Commerce 분리: 진행 시작
  - 완료: `PaymentCompletedFollowUpDispatcher`, 주문/기여 후속 변환 서비스 분리, Payment 직접 후속 이벤트 발행으로 back payment follow-up bridge 제거
  - 추가 시작: `inbox_event` 기반 idempotent consumer 토대 추가, `commerce.order.paid.v1` / `funding.contribution.created.v1` / `payment.failed.v1` inbox consumer scaffold 추가(기본 `disabled`)
  - 잔여: 보상 트랜잭션, 상태 테이블 분리, 직접 DB 참조 제거
- 5~6단계: 미착수

상세 체크박스는 `docs/MSA_MIGRATION_EXECUTION_ORDER.md`를 기준 문서로 사용한다.

## 3) 이번 단계에서 적용한 것
- Kafka 의존성 추가
- Outbox 패턴 도입
  - `outbox_event` 테이블
  - 트랜잭션 내 이벤트 적재
  - 스케줄러로 Kafka 비동기 발행
- 결제/펀딩 핵심 플로우에서 이벤트 생성
  - `funding.contribution.created.v1`
  - `commerce.order.paid.v1`
  - `payment.completed.v1`
  - `payment.failed.v1`
- Catalog ACL(anti-corruption layer) 1차 도입
  - `CatalogItemReader` 포트 추가
  - Funding/Commerce/Admin 서비스가 `ItemRepository` 직접 참조하지 않도록 치환
- Catalog 원격 어댑터 + 전환 플래그 도입
  - `APP_CATALOG_READER=local|remote`
  - `APP_CATALOG_REMOTE_BASE_URL`
  - `APP_CATALOG_REMOTE_FALLBACK_TO_LOCAL`
- Docker Compose에 Kafka 추가
  - 로컬: `kafka`, `kafka-ui`
  - 백엔드 배포: `kafka`
  - 현재 운영 이미지는 `apache/kafka:3.7.2`

## 4) 운영 체크리스트
- `APP_KAFKA_ENABLED=true`
- `KAFKA_BOOTSTRAP_SERVERS=kafka:9092`
- `APP_OUTBOX_ENABLED=true`
- outbox 적재/발행 모니터링:
  - `outbox_event.status` 분포 (`PENDING`, `RETRY`, `DEAD`, `PUBLISHED`)
  - `retry_count` 상위 이벤트 확인

## 5) 다음 단계 작업 지침 (순서대로)
1. Catalog 서비스 프로세스 분리
- 분리 대상 API: `/api/v1/items`, `/api/v3/items*`, `/api/v3/search`
- 기존 서버는 Catalog API Client를 통해 조회(HTTP)하고, 실패 시 폴백 정책을 둔다.
- 이유: 읽기 트래픽이 크고 타 도메인 의존도가 높아 먼저 분리하면 병목 완화 효과가 가장 크다.

2. Payment 서비스 프로세스 분리
- `payment` 스키마를 소유한 서비스로 분리하고, 결제 실행 결과는 Kafka 이벤트로 전파한다.
- 현재 운영은 Edge 기준 `/api/v1/pay/**`가 모두 Payment 서비스로 라우팅된다.
- 현재 구현은 Payment 서비스가 외부 `/api/v1/pay/**`를 전부 소유하고, `order/order-now/funding/friends` 쓰기와 finalize, contributor/barcode 영속성, 후속 도메인 이벤트 적재까지 직접 수행한다.
- Back 쪽 payment follow-up consumer는 비활성화되었고, Payment가 `payment.completed.v1`와 도메인 후속 이벤트를 함께 발행한다.
- 이유: 이미 별도 스키마/오케스트레이션이 있어 추출 비용이 낮다.

3. Funding/Commerce 트랜잭션 분리
- 동기 DB 조인을 제거하고 이벤트/Saga로 상태 동기화.
- 첫 구현은 `inbox_event`로 소비 이력과 중복 방지부터 시작한다.
- 이유: 최종적으로 서비스 간 분산 트랜잭션을 없애야 장애 전파를 막을 수 있다.

4. Admin/Home 읽기 모델 분리
- 조인성 조회를 Projection 테이블로 이전.
- 이유: CQRS 적용으로 조회 성능과 배포 독립성을 확보할 수 있다.
