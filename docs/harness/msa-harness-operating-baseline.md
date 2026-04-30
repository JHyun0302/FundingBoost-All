# MSA Harness Operating Baseline

## 기준일

- baseline created: 2026-04-30
- owner role: migration operator
- source reference:
  - `docs/MSA_MIGRATION_EXECUTION_ORDER.md`
  - `docs/HARNESS_ENGINEERING_START.md`
  - `docs/harness/*`

## 현재 서비스 경계

| 서비스 | 현재 책임 | 운영 상태 |
| --- | --- | --- |
| `FundingBoost-Server` | 회원, 펀딩, 커머스, 관리자, 홈, legacy bridge | 운영 중 |
| `FundingBoost-DataCrawler` | Catalog API, 상품 조회, 랭킹 조회 | Catalog 분리 완료 |
| `FundingBoost-Payment` | `/api/v1/pay/**`, 결제 실행, Payment outbox | Payment 분리 완료 |
| `Edge Nginx` | Catalog/Payment 라우팅, canary/full 전환 | Catalog/Payment full |
| `Kafka` | 도메인 이벤트 전달 | Outbox relay 기반 운영 |
| `MySQL` | 단일 DB 인스턴스, 스키마 단위 분리 | `fundingboost`, `item`, `payment` |

## 현재 마이그레이션 상태

| 단계 | 상태 | 기준 |
| --- | --- | --- |
| 1단계 모놀리스 내부 경계 고정 | 진행중 | 핵심 경로 완료, 잔여 내부 조회 의존 정리 남음 |
| 2단계 Catalog 서비스 분리 | 완료 | Edge full, back remote-only, fallback 제거 |
| 3단계 Payment 서비스 분리 | 완료 | Edge full, native write/finalize, Payment 직접 후속 이벤트 발행 |
| 4단계 Funding/Commerce Saga | 진행중 | inbox scaffold 완료, 상태 ownership/보상 트랜잭션 남음 |
| 5단계 Admin/Home Projection | 미착수 | projection 테이블/consumer 필요 |

## 현재 자동 하네스

| profile | 검증 범위 |
| --- | --- |
| `stage1-monolith-boundary` | 서버 컴파일, back compose, 이벤트 계약 문서 |
| `stage2-catalog-cutover` | server/crawler 컴파일, edge compose, Catalog 전환 플래그 |
| `stage3-payment-cutover` | server/payment 컴파일, edge/back compose, Payment 전환 플래그 |
| `stage4-saga-foundation` | 서버 컴파일, inbox 플래그, Saga 토픽 문서 |

## 반복 가능한 기준 명령

```bash
./scripts/harness/run.sh all
```

## 비밀값 처리

- `.env`, `deploy/oci/env/*.env`, SSH 키, DB 비밀번호는 커밋하지 않는다.
- 예시 파일은 `*.env.example`만 커밋한다.
- 운영 계정 검증 결과는 계정명과 결과만 남기고 토큰은 남기지 않는다.

## 기준선 위험

1. 4단계 Saga consumer는 아직 운영 활성화 전이다.
2. Funding/Commerce 상태 저장 ownership은 아직 완전히 분리되지 않았다.
3. Admin/Home 조회는 여전히 조인성 조회가 많아 projection 분리 전까지 변경 영향 범위가 크다.
4. 운영 스모크 일부는 QA 계정과 운영 VM 접근이 필요해 자동 하네스와 수동 하네스를 분리해야 한다.

## 작업 기록 규칙

- 마일스톤 작업 기록은 `docs/harness/YYYY-MM-DD-작업주제.md` 형식을 사용한다.
- 기록에는 목적, 변경 대상, 검증 결과, 다음 작업을 포함한다.
- 운영 전환이 있으면 canary 비율, fallback 상태, rollback 방법을 같이 남긴다.
