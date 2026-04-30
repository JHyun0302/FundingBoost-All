# FundingBoost MSA Harness Docs

## 목적

이 디렉토리는 FundingBoost MSA 마이그레이션을 하네스 엔지니어링 방식으로 진행하기 위한 기준 문서 모음이다.

YHPA 프로젝트의 문서 운영 방식에서 가져온 원칙은 다음과 같다.

1. 마일스톤마다 목표, 작업, 완료 조건, 중단 조건을 명시한다.
2. 가드레일은 코드 리뷰 의견이 아니라 merge 전 통과 조건으로 둔다.
3. 스모크 테스트는 운영 전환 직후 사람이 같은 순서로 재현할 수 있게 둔다.
4. 실행 결과는 별도 기록 템플릿에 남긴다.

## 문서 목록

| 문서 | 역할 |
| --- | --- |
| [msa-harness-operating-baseline.md](./msa-harness-operating-baseline.md) | 현재 운영 기준선과 책임 범위 |
| [msa-harness-milestones.md](./msa-harness-milestones.md) | 이후 MSA 마이그레이션 마일스톤 |
| [msa-harness-guardrails.md](./msa-harness-guardrails.md) | 변경 전 반드시 지켜야 하는 가드레일 |
| [msa-harness-smoke-tests.md](./msa-harness-smoke-tests.md) | 단계별 최소 스모크 테스트 |
| [msa-harness-rollout-checklist.md](./msa-harness-rollout-checklist.md) | 카나리, full 전환, rollback 체크리스트 |
| [msa-harness-release-readiness-template.md](./msa-harness-release-readiness-template.md) | release readiness 기록 템플릿 |

## 실행 명령

현재 자동 하네스는 아래 명령을 기준으로 사용한다.

```bash
./scripts/harness/run.sh list
./scripts/harness/run.sh all
./scripts/harness/run.sh stage4-saga-foundation
```

## 운영 원칙

- 새 MSA 작업을 시작하기 전에 먼저 [msa-harness-milestones.md](./msa-harness-milestones.md)의 대상 마일스톤을 고른다.
- 변경 전 [msa-harness-guardrails.md](./msa-harness-guardrails.md)를 확인한다.
- 변경 후 `./scripts/harness/run.sh <profile>`을 실행한다.
- 운영 전환이 있으면 [msa-harness-smoke-tests.md](./msa-harness-smoke-tests.md)와 [msa-harness-rollout-checklist.md](./msa-harness-rollout-checklist.md)를 함께 사용한다.
- 마일스톤을 닫기 전 [msa-harness-release-readiness-template.md](./msa-harness-release-readiness-template.md) 형식으로 결과를 남긴다.
