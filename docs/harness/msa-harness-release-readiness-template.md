# MSA Harness Release Readiness Template

## Summary

- milestone:
- review date:
- operator:
- environment:
- readiness result: `READY` / `PARTIAL` / `BLOCKED`

## Scope

- changed service:
- changed route:
- changed event:
- changed env flag:
- affected API:

## Harness Review

- related profile:
- `./scripts/harness/run.sh <profile>` result: `PASS` / `FAIL`
- `./scripts/harness/run.sh all` result: `PASS` / `FAIL` / `SKIPPED`
- artifact path:
- notes:

## Smoke Review

- smoke document used: `docs/harness/msa-harness-smoke-tests.md`
- QA accounts used:
- forced route checked: `YES` / `NO`
- canary route checked: `YES` / `NO`
- full route checked: `YES` / `NO`
- rollback checked: `YES` / `NO`
- notes:

## Event Review

- outbox event created: `YES` / `NO`
- inbox event consumed: `YES` / `NO`
- duplicate delivery checked: `YES` / `NO`
- failed event handling checked: `YES` / `NO`
- notes:

## Data Ownership Review

- cross-service write added: `YES` / `NO`
- cross-service read added: `YES` / `NO`
- ownership table updated: `YES` / `NO`
- remaining direct dependency:
- removal milestone:

## Operational Review

- route mode:
- canary percent:
- fallback status:
- observed 5xx:
- observed p95 latency:
- log errors:
- rollback command:

## Blocking Risks

- risk 1:
- risk 2:
- risk 3:

If no material blockers remain, write:

- none

## Decision

- ready for next canary step: `YES` / `NO`
- ready for full route: `YES` / `NO`
- ready to remove fallback: `YES` / `NO`
- required action before next step:

## References

- [msa-harness-milestones.md](./msa-harness-milestones.md)
- [msa-harness-guardrails.md](./msa-harness-guardrails.md)
- [msa-harness-smoke-tests.md](./msa-harness-smoke-tests.md)
- [msa-harness-rollout-checklist.md](./msa-harness-rollout-checklist.md)
