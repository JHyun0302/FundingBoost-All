# 하네스 엔지니어링 시작점

## 한 줄 정의
- 작업 종류별 통과 조건을 문서화하고, 그 조건을 같은 명령 순서로 실행해, 누가 돌려도 같은 PASS/FAIL이 나오게 만드는 것.

## 왜 지금 필요한가
- 지금 리포는 모노레포 안에서 `FundingBoost-Server`, `FundingBoost-DataCrawler`, `FundingBoost-Payment`, `deploy`를 함께 바꾸며 MSA로 나누고 있다.
- 이 상태에서 사람마다 다른 검증 습관으로 확인하면, 같은 변경도 누군가는 통과시키고 누군가는 놓친다.
- 그래서 먼저 `작업 종류 -> 통과 조건 -> 실행 명령 -> 증거 로그`를 고정해야 한다.

## 이 리포의 하네스 최소 단위
- `profile`
  - 하나의 작업 종류를 뜻한다.
  - 예: `stage2-catalog-cutover`, `stage3-payment-cutover`
- `required file`
  - 그 작업이 성립하려면 반드시 있어야 하는 코드/설정 파일이다.
- `check`
  - 실제로 실행되는 명령이다.
  - 성공/실패는 exit code로 고정한다.
- `artifact`
  - 실행 로그다.
  - `artifacts/harness/<timestamp>/` 아래에 남긴다.

## 지금 추가한 프로파일
| profile | 의미 | 통과 기준 |
| --- | --- | --- |
| `stage1-monolith-boundary` | 서비스 분리 전 내부 경계 고정 | 서버 컴파일, back compose, 이벤트 계약/체크리스트 문서 |
| `stage2-catalog-cutover` | Catalog 조회 경로 분리 | server/crawler 컴파일, edge compose, Catalog 플래그 문서 |
| `stage3-payment-cutover` | Payment 서비스 분리 | server/payment 컴파일, edge/back compose, Payment 플래그 문서 |
| `stage4-saga-foundation` | Funding/Commerce Saga 기반 | 서버 컴파일, inbox 플래그/토픽 문서, back compose |

## 실행 방법
```bash
./scripts/harness/run.sh list
./scripts/harness/run.sh stage2-catalog-cutover
./scripts/harness/run.sh stage1-monolith-boundary stage2-catalog-cutover stage3-payment-cutover stage4-saga-foundation
./scripts/harness/run.sh all
```

## MSA 마일스톤 문서

- [harness/README.md](./harness/README.md)
- [harness/msa-harness-operating-baseline.md](./harness/msa-harness-operating-baseline.md)
- [harness/msa-harness-milestones.md](./harness/msa-harness-milestones.md)
- [harness/msa-harness-guardrails.md](./harness/msa-harness-guardrails.md)
- [harness/msa-harness-smoke-tests.md](./harness/msa-harness-smoke-tests.md)
- [harness/msa-harness-rollout-checklist.md](./harness/msa-harness-rollout-checklist.md)
- [harness/msa-harness-release-readiness-template.md](./harness/msa-harness-release-readiness-template.md)

## 이 리포에서 정한 규칙
1. 작업 종류가 생기면 먼저 profile부터 만든다.
2. PR/커밋에서 기준 문서와 실행 명령이 같이 바뀌어야 한다.
3. 수동 QA만 있는 작업이라도, 최소한 플래그/토픽/compose/config 검증은 profile에 넣는다.
4. 운영 검증이 필요한 작업은 `문서화된 수동 체크리스트`와 `자동 명령`을 분리한다.
5. 같은 종류의 변경은 항상 같은 profile로 검증한다.

## 지금 당장 얻는 효과
- `Catalog`, `Payment`, `Saga` 변경을 더 이상 기억에 의존해 확인하지 않는다.
- compile/compose/config/doc drift가 생기면 한 명령으로 바로 잡힌다.
- 새 사람이 들어와도 "무엇을 통과해야 merge 가능한가"를 바로 알 수 있다.

## 현재 드러난 환경 차이 보정
- `FundingBoost-Payment`는 wrapper가 없어서 로컬 `gradle` 상태에 따라 결과가 달랐다.
- repeatable harness를 위해 `FundingBoost-Payment/gradlew`와 wrapper 파일을 추가했다.
- 이제 Payment 검증도 `./gradlew -q compileJava` 기준으로 고정할 수 있다.

## 다음 확장 순서
1. `qa`용 API 검증 스크립트를 profile check로 편입
2. 운영 canary 관찰 명령을 `manual gate`와 `auto gate`로 분리
3. 5단계 Projection용 profile 추가
4. merge 전에 `./scripts/harness/run.sh all`을 기본 게이트로 사용
