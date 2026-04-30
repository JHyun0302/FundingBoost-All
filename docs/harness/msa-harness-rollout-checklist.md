# MSA Harness Rollout Checklist

## 목적

이 문서는 MSA 마이그레이션 운영 전환 순서를 고정한다.

사용 시점:

- Edge route mode 변경
- Kafka consumer 활성화
- service ownership 이동
- fallback 제거

## Rollout Environments

권장 순서:

1. local compose
2. isolated QA path with forced route header
3. canary production traffic
4. full production route
5. fallback removal

## Pre-Release Checks

전환 전 확인:

- 관련 harness profile 통과
- `*.env.example`에 새 플래그 반영
- 운영 env에 새 플래그 값 확인
- outbox relay running
- Kafka topic and consumer group 확인
- rollback 플래그 확인
- smoke test 대상 QA 계정 확인

## Stage 1. Forced Route Validation

목표:

- 실제 사용자 트래픽을 바꾸기 전에 강제 헤더로 새 경로만 검증한다.

확인:

- `X-Catalog-Route: catalog`
- `X-Payment-Route: payment`
- route response header
- status/body parity

rollback:

- 강제 헤더 제거

## Stage 2. Canary

목표:

- 일부 트래픽만 새 경로로 보낸다.

권장 비율:

- `5 -> 10 -> 25 -> 50 -> 100`

확인:

- 5xx rate
- p95 latency
- route distribution
- service logs
- outbox/inbox lag
- QA critical path

rollback:

- route mode를 `off`로 변경
- consumer flag를 `false`로 변경

## Stage 3. Full Route

목표:

- 대상 API의 모든 외부 트래픽을 새 서비스로 보낸다.

확인:

- natural traffic route header is new service
- forced old route still works only if rollback path is intentionally kept
- fallback is still explicit
- no hidden fallback is masking failure

rollback:

- route mode를 이전 값으로 복구

## Stage 4. Fallback Removal

목표:

- 안정화 후 fallback을 제거해 장애를 숨기지 않게 한다.

확인:

- full route 안정화 완료
- smoke test 통과
- logs clean
- outbox/inbox lag acceptable

rollback:

- fallback flag를 다시 켜고 재기동
- 필요한 경우 route mode를 `off`로 복구

## Release Gates

모두 참이어야 다음 단계로 간다.

- 자동 harness profile 통과
- 운영 smoke 통과
- rollback 명령 확인
- 신규 이벤트 중복 없음
- QA 계정 대표 경로 통과
- 문서와 env example이 최신 상태

## Stop Conditions

아래 상황이면 즉시 rollout을 멈춘다.

- 5xx가 baseline보다 증가한다.
- p95 latency가 설명 없이 악화된다.
- route distribution이 설정과 다르다.
- 이벤트 중복 처리로 상태가 두 번 변한다.
- fallback이 성공처럼 보이게 만든다.
- rollback 후에도 새 consumer가 계속 처리한다.

## Output

전환 후 [msa-harness-release-readiness-template.md](./msa-harness-release-readiness-template.md)에 결과를 남긴다.
