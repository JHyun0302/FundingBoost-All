# MSA Harness Milestones

## 목적

이 문서는 4단계 이후 MSA 마이그레이션을 마일스톤 단위로 쪼개기 위한 실행 기준이다.

핵심 원칙은 다음과 같다.

1. 마일스톤 하나는 독립적으로 검증 가능해야 한다.
2. 운영 트래픽 전환이 있는 마일스톤은 canary, smoke, rollback을 포함해야 한다.
3. 서비스 ownership 이동은 DB 물리 분리보다 먼저 이벤트와 쓰기 책임으로 증명해야 한다.

## M0. Harness Baseline Freeze

목표:

- 현재 1~4단계 상태를 하네스 기준선으로 고정한다.

작업:

- `docs/harness/*` 문서 세트 작성
- `./scripts/harness/run.sh all` 통과 확인
- `FundingBoost-Payment` wrapper 기준 고정
- 기존 운영 플래그와 라우팅 상태 기록

완료 조건:

- 하네스 문서와 프로파일이 리포에 존재한다.
- 전체 자동 하네스가 통과한다.
- 이후 작업자가 같은 명령으로 같은 기준을 재현할 수 있다.

중단 조건:

- profile이 특정 개인 환경에서만 동작한다.
- compile/config/doc 검증 중 하나라도 기록 없이 생략된다.

## M1. Saga Inbox Activation

목표:

- `commerce.order.paid.v1`, `funding.contribution.created.v1`, `payment.failed.v1` 소비를 inbox 기반으로 운영 검증한다.

작업:

- `APP_INBOX_CONSUMERS_ENABLED=true` 운영 전환 준비
- consumer group, topic, retry 정책 확인
- inbox 중복 소비 방지 검증
- payment outbox 이벤트를 실제 Saga consumer가 받아도 중복 상태 변경이 없는지 확인

완료 조건:

- QA 이벤트 1건이 inbox에 기록되고 중복 재처리해도 결과가 동일하다.
- consumer 활성화 후 기존 Payment full 경로가 유지된다.
- 실패 시 `APP_INBOX_CONSUMERS_ENABLED=false` rollback이 확인된다.

중단 조건:

- 같은 이벤트가 중복 상태 변경을 만든다.
- consumer 실패가 Payment API 응답 경로를 지연시키거나 실패시킨다.
- rollback 후에도 consumer가 계속 메시지를 처리한다.

## M2. Funding/Commerce State Ownership

목표:

- Funding/Commerce 상태 반영 책임을 이벤트 소비 중심으로 재배치한다.

작업:

- `commerce.order.paid.v1` 처리 책임을 Commerce handler로 고정
- `funding.contribution.created.v1` 처리 책임을 Funding handler로 고정
- Payment 서비스가 Funding/Commerce 테이블을 직접 갱신하는 범위 축소
- 상태 변경 idempotency key 정의

완료 조건:

- Payment는 결제 사실과 후속 이벤트를 발행하고, Funding/Commerce 상태는 각 handler가 반영한다.
- 같은 이벤트를 재전송해도 order/funding/contributor 상태가 한 번만 바뀐다.
- 상태 반영 실패는 inbox에 실패 상태로 남고 재처리 가능하다.

중단 조건:

- Payment와 Funding/Commerce가 같은 상태 컬럼을 동시에 갱신한다.
- 이벤트 순서가 바뀌면 영구 불일치가 발생한다.
- 보상 규칙 없이 실패 상태를 숨긴다.

## M3. Compensation Transactions

목표:

- `payment.failed.v1`와 후속 상태 반영 실패에 대한 보상 규칙을 만든다.

작업:

- 결제 실패, 주문 생성 실패, 펀딩 기여 실패 케이스 분류
- 보상 이벤트 이름과 payload 정의
- 실패 상태 조회/재처리 운영 절차 작성
- 보상 시 중복 포인트 복원, 주문 취소, 기여 취소 방지 검증

완료 조건:

- 실패 이벤트가 상태별로 하나의 보상 경로를 가진다.
- 보상 재시도는 idempotent하다.
- 운영자가 실패 inbox/outbox를 조회하고 재처리할 수 있다.

중단 조건:

- 보상 로직이 원래 성공 로직보다 더 많은 상태를 직접 만진다.
- 실패 이벤트 payload만으로 원인과 대상 식별이 불가능하다.

## M4. Cross-Service DB Reference Reduction

목표:

- 서비스 간 DB 직접 참조를 줄이고 contract/API/event 경계로 바꾼다.

작업:

- Payment, Catalog, Funding, Commerce 기준 소유 테이블 목록 작성
- 다른 서비스 스키마 직접 조회 목록 수집
- 조회 전용은 API/read model로 대체
- 쓰기 경로는 이벤트 또는 internal command로 제한

완료 조건:

- 새 코드는 다른 서비스 소유 테이블을 직접 쓰지 않는다.
- 잔여 직접 조회는 문서에 이유와 제거 예정 마일스톤이 있다.
- service ownership 표가 최신 상태다.

중단 조건:

- 빠른 구현을 위해 다른 스키마 write가 추가된다.
- 소유권 불명 테이블이 새로 생긴다.

## M5. Admin/Home Projection

목표:

- Admin/Home 조인성 조회를 이벤트 기반 projection으로 분리한다.

작업:

- Admin/Home 화면별 필요한 read model 정의
- projection table bootstrap
- payment/funding/commerce/catalog 이벤트 consumer 작성
- 기존 조회와 projection 조회 parity 검증

완료 조건:

- 주요 Admin/Home API가 projection 조회로 응답 가능하다.
- projection lag, rebuild 절차, fallback 조회가 문서화되어 있다.
- 기존 조인 조회 대비 응답 스펙이 유지된다.

중단 조건:

- projection이 source-of-truth처럼 직접 수정된다.
- rebuild 없이 데이터 누락을 수동 보정해야 한다.

## M6. Observability And Release Readiness

목표:

- MSA 운영 전환 후 문제를 빠르게 발견하고 되돌릴 수 있게 한다.

작업:

- route mode, error rate, latency, outbox/inbox lag 지표 정의
- smoke test 명령과 운영 체크리스트 정리
- release readiness template으로 마일스톤 완료 기록
- rollback 절차 리허설

완료 조건:

- 장애 판단 기준이 수치 또는 명령으로 표현된다.
- rollback 명령이 문서에 있고 실제로 검증됐다.
- 운영 전환 전 readiness 기록이 남는다.

중단 조건:

- 로그만 보고 성공 여부를 판단한다.
- rollback 경로가 없거나 실행자가 알 수 없다.

## 현재 권장 실행 순서

1. `M0. Harness Baseline Freeze`
2. `M1. Saga Inbox Activation`
3. `M2. Funding/Commerce State Ownership`
4. `M3. Compensation Transactions`
5. `M4. Cross-Service DB Reference Reduction`
6. `M5. Admin/Home Projection`
7. `M6. Observability And Release Readiness`
