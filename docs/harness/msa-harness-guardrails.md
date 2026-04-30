# MSA Harness Guardrails

## 목적

이 문서는 FundingBoost MSA 마이그레이션 중 반드시 지켜야 하는 가드레일이다.

가드레일은 권장사항이 아니라 작업 중단 기준이다. 어기면 구현을 멈추고 설계를 다시 잡는다.

## 공통 가드레일

- DB 인스턴스는 하나만 유지하고 스키마 단위로만 분리한다.
- 서비스 간 쓰기 정합성은 Outbox와 idempotent consumer로 보장한다.
- 이벤트 이름은 버전을 포함한다. 예: `payment.completed.v1`
- consumer는 같은 메시지를 여러 번 받아도 같은 결과가 나와야 한다.
- 운영 전환은 `off -> canary -> full` 순서를 따른다.
- canary 또는 full 전환에는 rollback 플래그가 있어야 한다.
- 비밀값은 tracked file에 넣지 않는다.

## 서비스 ownership 가드레일

| 영역 | 소유자 | 금지 |
| --- | --- | --- |
| Catalog 상품 조회 | `FundingBoost-DataCrawler` | Back 서버에서 Catalog write 추가 |
| Payment 결제 실행 | `FundingBoost-Payment` | Back 서버에서 신규 Payment write 추가 |
| Funding 상태 | Funding domain | Payment가 Funding 상태를 최종 소유하는 변경 |
| Commerce 주문 상태 | Commerce domain | Payment가 Commerce 상태를 최종 소유하는 변경 |
| Admin/Home 읽기 모델 | Projection | Projection 테이블을 source-of-truth처럼 수정 |

## 이벤트 가드레일

- event payload에는 대상 식별자가 반드시 있어야 한다.
- `eventType + eventKey`는 같은 비즈니스 사실을 한 번만 가리켜야 한다.
- outbox insert 전 중복 방지 guard 또는 DB unique 제약을 둔다.
- inbox는 topic, partition, offset 또는 event id 기준 중복 처리를 막아야 한다.
- 실패 이벤트는 성공 이벤트와 payload 계약을 공유하지 않는다. 실패 이유와 재시도 가능 여부를 분리한다.

## API/응답 호환성 가드레일

- 기존 클라이언트가 사용하는 field는 마일스톤 안에서 제거하지 않는다.
- API 이관 시 상태 코드와 주요 response body parity를 먼저 확인한다.
- 오류 응답에도 service mode 또는 route 확인 헤더를 유지한다.
- 운영 스모크에서 `qa1~qa5` 계정 기준 대표 경로를 확인한다.

## 배포/라우팅 가드레일

- Edge route mode는 문서화된 값만 사용한다. 예: `off`, `canary`, `full`
- canary 비율 변경 전후에 5xx, latency, route 분포를 확인한다.
- `full` 전환 후 fallback 제거는 별도 단계로 진행한다.
- fallback이 켜진 상태를 장기 운영 상태로 남기지 않는다.

## 코드 구조 가드레일

- 새 기능을 기존 대형 service에 직접 누적하지 않는다.
- 외부 서비스 호출은 application service 뒤 adapter로 둔다.
- controller는 orchestration을 늘리지 않고 facade 또는 application boundary로 위임한다.
- 테스트 또는 초기화용 경로가 운영 경계를 우회하지 않게 한다.

## Stop Conditions

아래 상황이면 해당 마일스톤 작업을 중단한다.

- 이벤트 중복 소비가 실제 상태 중복 변경을 만든다.
- rollback 플래그가 없는 운영 전환이 필요하다.
- 다른 서비스 소유 스키마에 신규 write가 추가된다.
- QA 스모크가 실패했는데 fallback 때문에 성공처럼 보인다.
- 문서의 완료 조건 없이 코드만 먼저 merge해야 하는 상황이 된다.
