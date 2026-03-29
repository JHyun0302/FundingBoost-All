# FundingBoost 마이그레이션 실행 순서 (체크리스트)

기준일: 2026-03-25

## 원칙
- DB 서버는 1개(MySQL 인스턴스 1개) 유지
- 스키마만 분리: `fundingboost`, `item`, `payment`
- 서비스 간 상태 전달은 Kafka 이벤트 중심
- 쓰기 정합성은 Outbox 패턴으로 보장

## 단계별 진행 현황 (요약)
- 1단계 모놀리스 내부 경계 고정: 진행중 (핵심 경로 완료, 잔여 내부 의존 정리 남음)
- 2단계 Catalog 서비스 분리: 완료 (edge full 전환, back remote-only 전환, QA 검증 완료)
- 3단계 Payment 서비스 분리: 완료 (edge full 전환, Payment native write/finalize ownership 이전 완료, Payment 직접 후속 이벤트 발행 확인, back follow-up consumer 제거)
- 4단계 Funding/Commerce 분리(Saga): 진행중 (보상 트랜잭션/상태 ownership 정리 남음)
- 5단계 Admin/Home Projection 분리: 미착수

## 1단계: 모놀리스 내부 경계 고정 (이유: 이후 분리 시 변경 범위 축소)
- [x] Outbox + Kafka 발행 인프라 도입
- [x] 핵심 이벤트 발행 시작 (`funding.contribution.created.v1`, `commerce.order.paid.v1`)
- [x] Payment 전용 스키마(`payment`) 운용
- [x] `CatalogItemReader` 포트 도입 및 주요 서비스 치환
- [x] Catalog 원격 어댑터 + feature flag (`local|remote`) + local fallback
- [ ] 잔여 내부 조회 의존 정리 (예: 인덱싱/테스트 초기화 경로)

## 2단계: Catalog 서비스 분리 (이유: 읽기 트래픽이 커서 효과가 가장 빠름)
- [x] Catalog 스켈레톤 API 준비 (`/api/v1/items`, `/api/v3/*`)
- [x] 기존 서버에 Catalog API Client 어댑터 연결 (`RemoteCatalogItemReader`)
- [x] 전환 전 필수 갭 처리
  - [x] item detail bookmark 정확도(JWT `sub` 기반 조회)
  - [x] 응답 스펙 호환성(상세/목록/스냅샷 조회)
- [x] 엣지(Nginx) 라우팅 규칙 반영 (`/api/v1/items`, `/api/v3/*`)
- [x] 점진 전환 설정 반영 (`off|canary|full`, canary 비율)
- [x] 운영 카나리 시작 (5%)
- [x] 운영 카나리 확대 (25%)
- [x] 운영 카나리 확대 (50% -> 100%)
- [x] `CATALOG_ROUTE_MODE=full`
- [x] Back `APP_CATALOG_REMOTE_FALLBACK_TO_LOCAL=false`
- [x] 전환 후 개선
  - [x] audience 랭킹 필터
  - [x] 정교한 랭킹 점수(펀딩/주문/위시 집계)

## 3단계: Payment 서비스 분리 (이유: 이미 스키마/오케스트레이션이 있어 추출 비용 낮음)
- [x] Payment API facade 도입 (`PayController -> PaymentFacade`)
- [x] Payment 전용 프로세스 스켈레톤 생성 (`FundingBoost-Payment`, `:8081`)
- [x] Edge `/api/v1/pay` 라우팅 스위치 준비 (`PAYMENT_ROUTE_MODE=off|canary|full`)
- [x] Payment 후속 이벤트 consumer scaffold 분리 (기본 `disabled`)
- [x] `/api/v1/pay/*` 1차 이관
  - [x] Payment 서비스가 `/api/v1/pay/**`를 수신
  - [x] Payment -> back-server proxy 어댑터 적용
  - [x] QA 계정(`qa1~qa5`) 기준 조회 경로 동일성 검증 (`view/order`, `view/funding/*`, `friends/*`)
- [x] Payment 점진 전환 시작
  - [x] `PAYMENT_ROUTE_MODE=canary`
  - [x] `PAYMENT_CANARY_PERCENT=5`
  - [x] 샘플링 기준 5% 분포 확인 (`payment=7`, `back=93`)
- [x] `/api/v1/pay` POST 진입점 2차 이관
  - [x] Payment 서비스가 `POST /api/v1/pay/**`를 명시적으로 수신
  - [x] Payment -> back-server internal command adapter (`/internal/payment/commands/**`)
  - [x] QA 기준 쓰기/친구결제 POST 경로 검증
- [x] Outbox relay/Kafka 운영 보정
  - [x] Kafka 이미지 교체 (`apache/kafka:3.7.2`)
  - [x] relay 트랜잭션 버그 수정 (`OutboxRelayService`)
  - [x] `payment.completed.v1`, `commerce.order.paid.v1` 실제 `PUBLISHED` 확인
- [x] 결제 성공/실패 이벤트 계약 정의
  - [x] `payment.completed.v1`
  - [x] `payment.failed.v1`
- [x] Commerce/Funding 후속 처리 consumer-mode 운영 전환
  - [x] `payment.completed.v1` -> `commerce.order.paid.v1` 변환 consumer
  - [x] `payment.completed.v1` -> `funding.contribution.created.v1` 변환 consumer
  - [x] Back `APP_PAYMENT_FOLLOW_UP_MODE=consumer`
  - [x] Back `APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED=true`
- [x] Payment canary 확대
  - [x] `PAYMENT_CANARY_PERCENT=25`
  - [x] 샘플링 기준 25% 분포 확인 (`payment=26`, `back=74`)
- [x] Payment native write 1차 실이관
  - [x] `POST /api/v1/pay/order/now` Payment 서비스 내부 실행
  - [x] `POST /api/v1/pay/order` Payment 서비스 내부 실행
  - [x] Payment 스키마 `payment_intent` / `payment_attempt` / `payment.outbox_event` 사용 확인
  - [x] idempotency 재호출 시 주문/결제 intent/후속 이벤트 중복 미생성 확인
  - [x] Payment native write 2차 실이관
  - [x] `POST /api/v1/pay/funding/{fundingItemId}` native 실행
  - [x] Catalog 분리 이후 `funding_item.item_id`를 Catalog reader로 해석하도록 보정
  - [x] `POST /api/v1/pay/friends/{fundingId}` native 진입점 검증
  - [x] `POST /api/v1/pay/friends/{fundingId}/barcode-token` native 진입점 검증
  - [x] `POST /api/v1/pay/friends/{fundingId}/barcode-token/consume` native 진입점 검증
  - [x] Payment 서비스 내부 contributor/barcode 영속성 실행
  - [x] back JWT 재사용용 internal auth bridge (`/internal/payment/auth/me`)
  - [x] QA 계정 기준 성공 경로 검증
    - [x] `qa2 -> funding 27` 친구결제 성공
    - [x] `qa1 -> fundingItem 27` 남은 금액 결제 성공
    - [x] `qa2 -> funding 28` barcode issue / consume 성공
    - [x] `qa1~qa5` invalid friend write parity 확인
    - [x] `qa2 -> funding 28` direct friend 결제 성공
    - [x] Payment `payment.completed.v1` -> Back `funding.contribution.created.v1` `PUBLISHED` 확인
- [x] Payment 이벤트 중복 방지 보정
  - [x] `attachOrderIdIfAbsent` 기반 `payment.completed.v1` 1회 발행
  - [x] Back `commerce.order.paid.v1` dedupe guard 추가
- [x] Payment finalize 경계 축소
  - [x] `PaymentOrderFinalizeService`로 `order`, `order/now`, `funding/*` 최종 주문 생성/포인트 차감/펀딩 완료 처리 이동
  - [x] `NativeOrderPaymentService`, `NativeFundingPaymentService`가 back finalize bridge 대신 Payment 내부 finalize 사용
  - [x] `native-friend`, `native-order`, `native-funding` 오류 응답에도 `X-Payment-Service-Mode` 헤더 보장
- [x] `payment` 스키마 소유권을 Payment 서비스로 이동
  - [x] `payment_intent`, `payment_attempt`, `payment.outbox_event`
  - [x] 기여/바코드 영속성(`contributor`, `friend_pay_barcode_token`)을 Payment 서비스 실행 경로로 이전
  - [x] Payment가 `commerce.order.paid.v1`, `funding.contribution.created.v1`를 직접 적재
- [x] 결제 오케스트레이션/쓰기 로직을 Payment 서비스로 실이관
  - [x] `order`, `order/now`
  - [x] `funding/*`
  - [x] `friends/*`
- [x] Payment canary 확대 (`100% -> full`)
  - [x] `PAYMENT_CANARY_PERCENT=50`
  - [x] `PAYMENT_CANARY_PERCENT=100`
  - [x] `PAYMENT_ROUTE_MODE=full`
- [x] Payment 후속 상태 ownership 이전 완료
  - [x] Payment가 `payment.completed.v1`와 도메인 후속 이벤트를 함께 발행
  - [x] Back `APP_PAYMENT_FOLLOW_UP_MODE=direct`
  - [x] Back `APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED=false`
  - [x] 2026-03-25 QA 검증 후 Payment outbox에만 신규 `commerce.order.paid.v1` / `funding.contribution.created.v1` 적재 확인

## 4단계: Funding/Commerce 분리 (Saga) (이유: 서비스 간 DB 직접 의존 제거)
- [x] `payment.completed.v1` 후속 변환 책임 분리
  - [x] `PaymentCompletedFollowUpDispatcher`
  - [x] `OrderPaymentCompletedFollowUpService`
  - [x] `FundingContributionPaymentCompletedFollowUpService`
- [x] Payment 직접 후속 이벤트 발행으로 back follow-up bridge 제거
- [x] `funding.contribution.created.v1` key를 contribution 단위(`fundingId:contributorId`)로 보정
- [ ] 교차 서비스 동기 호출 최소화
- [ ] Saga 보상 트랜잭션 설계/적용
- [ ] 서비스 간 DB 직접 참조 제거

## 5단계: Admin/Home Projection 분리 (이유: 조인성 조회를 읽기 모델로 분리)
- [ ] Kafka 소비자 기반 Projection 테이블 구축
- [ ] Admin/Home 조회를 Projection 전용으로 전환
- [ ] 대시보드 조인 쿼리 제거

## 지금 바로 진행할 일 (권장 순서)
1. Funding/Commerce Saga 본격화
   - 서비스별 보상/재처리 규칙과 상태 테이블 분리
2. 교차 서비스 동기 호출 최소화
   - Payment 이후 상태 반영을 이벤트 소비 중심으로 더 축소
3. Admin/Home Projection 분리 준비
   - 읽기 모델 이벤트 적재/조회 전환

## QA 정리 상태
- [x] 2026-03-25 Payment QA 데이터 정리
  - [x] `orders 9~17`
  - [x] 연관 `order_item`
  - [x] 연관 `payment_intent`, `payment_attempt`
  - [x] 연관 `payment.outbox_event`, `fundingboost.outbox_event`
  - [x] `funding 28`, `funding_item 28`, `contributor`, `friend_pay_barcode_token`

## Kafka 초보 운영 규칙 (짧게)
- Producer는 "사실(event fact)"만 보낸다.
- Consumer는 "여러 번 받아도 같은 결과"가 되게 만든다(idempotent).
- 실패 이벤트는 재시도하고, 끝까지 실패하면 `DEAD` 상태로 보관한다.
- 이벤트 이름은 버전 포함(`*.v1`)으로 시작한다.
