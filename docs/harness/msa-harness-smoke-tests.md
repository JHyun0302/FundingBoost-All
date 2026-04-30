# MSA Harness Smoke Tests

## 목적

이 문서는 MSA 마이그레이션 단계별 최소 스모크 테스트를 정의한다.

자동 하네스는 compile/config/doc drift를 잡고, 이 문서는 운영 API와 상태 전이를 확인한다.

## 공통 전제

환경 변수:

```bash
BASE_URL="http://<EDGE_HOST>"
BACK_URL="http://<BACK_HOST>:8080"
CATALOG_URL="http://<BACK_HOST>:8090"
PAYMENT_URL="http://<BACK_HOST>:8081"
```

QA 계정:

- `qa1`
- `qa2`
- `qa3`
- `qa4`
- `qa5`

공통 실패 기준:

- 5xx가 발생한다.
- route header가 기대 경로와 다르다.
- fallback header가 켜져 있는데 성공으로 기록한다.
- response body parity가 깨진다.
- outbox/inbox에 중복 이벤트가 생긴다.

## 1. Catalog Smoke

사용 시점:

- Catalog API 변경
- `/api/v1/items`, `/api/v3/*` 라우팅 변경
- Catalog ranking/bookmark 변경

명령:

```bash
curl -sS -D - "$BASE_URL/api/v3/items?size=1" -o /tmp/catalog-list.json
curl -sS -D - "$BASE_URL/api/v3/items/1" -o /tmp/catalog-detail.json
curl -sS -D - -H "X-Catalog-Route: catalog" "$BASE_URL/api/v3/items?size=1" -o /tmp/catalog-forced.json
```

기대 결과:

- status is `200`
- `X-Catalog-Route` is `catalog` when forced
- bookmark field exists for authenticated detail smoke
- ranking endpoint returns non-empty items for realistic filters

## 2. Payment Smoke

사용 시점:

- `/api/v1/pay/**` 변경
- Payment native flow 변경
- payment outbox 변경

명령:

```bash
curl -sS -D - -H "X-Payment-Route: payment" "$BASE_URL/api/v1/pay/view/order" -o /tmp/payment-view-order.json
curl -sS -D - "$PAYMENT_URL/internal/payment/health" -o /tmp/payment-health.json
```

기대 결과:

- payment health is `UP`
- forced route has `X-Payment-Route: payment`
- response body is compatible with existing client
- native write success path emits `payment.completed.v1`
- idempotency key replay does not create duplicate order, intent, or outbox row

## 3. Saga Inbox Smoke

사용 시점:

- `APP_INBOX_CONSUMERS_ENABLED=true`
- Saga handler 변경
- Funding/Commerce state ownership 이동

명령 예시:

```bash
./scripts/harness/run.sh stage4-saga-foundation
```

운영 확인:

- `commerce.order.paid.v1` is consumed once
- `funding.contribution.created.v1` is consumed once
- `payment.failed.v1` is recorded or handled according to the failure policy
- duplicate delivery does not mutate state twice

기대 결과:

- inbox row is created for each consumed event
- processed event has deterministic status
- failed event keeps reason and retry information

## 4. Projection Smoke

사용 시점:

- Admin/Home projection table 추가
- projection consumer 활성화
- Admin/Home API 조회 경로 전환

명령 예시:

```bash
curl -sS -D - "$BASE_URL/api/v1/home" -o /tmp/home.json
curl -sS -D - "$BASE_URL/api/v1/admin/dashboard" -o /tmp/admin-dashboard.json
```

기대 결과:

- 기존 API status and body shape are preserved
- projection data is not empty for seeded or QA state
- projection lag is visible or documented
- fallback query path is known

## 5. Rollback Smoke

사용 시점:

- canary 비율 변경
- route mode full 전환
- consumer 활성화

확인 항목:

- Edge route mode can return to previous state
- consumer can be disabled by env flag
- fallback behavior is explicit
- route headers prove rollback path

기대 결과:

- rollback 후 API status is stable
- disabled consumer stops new processing
- pending outbox/inbox rows are not lost

## 기록 형식

스모크 테스트 결과는 아래 형식으로 남긴다.

```markdown
## Smoke Record

- date:
- operator:
- milestone:
- environment:
- route mode:
- command:
- result:
- evidence:
- rollback checked: YES / NO
- next action:
```
