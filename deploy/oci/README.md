# OCI 3-VM 배포 가이드 (Edge 1 + Front 1 + Back 1)

## 0) 대상 구성
- `Edge VM (AMD micro)`: `docker-compose.edge.yml` 실행, 공인 IP 부여
- `Front VM (ARM 2 OCPU / 12GB)`: `docker-compose.front.yml` 실행
- `Back VM (ARM 2 OCPU / 12GB)`: `docker-compose.back.yml` 실행

현재 VM IP (질문에서 공유한 값):
- `Edge`: Public `138.2.43.7`, Private `10.0.0.252`
- `Front`: Private `10.0.1.198`
- `Back`: Private `10.0.1.62`

권장 디스크(무료 200GB 맞춤):
- `Edge 50GB`
- `Front 75GB`
- `Back 75GB`

총 `200GB`.

## 1) 핵심 네트워크 정책
- 공인 IP는 `Edge`에만 붙임.
- `Front`, `Back`은 Public IP 없이 Private 서브넷에 둠.
- 사용자 트래픽은 항상 `Edge -> Front/Back`으로만 이동.

NSG/Security List 최소 규칙:
- Edge Inbound: `80/tcp` (전체), `22/tcp` (내 IP만)
- Front Inbound: `80/tcp` (Edge Private IP 또는 Edge Subnet CIDR만)
- Back Inbound: `8080/tcp` (Edge/Front에서 API 접근), `8090/tcp` (필요 시 Edge/Front에서 crawler 접근)
- Back Outbound: 외부 API/Kakao 호출용 `443` 허용

참고:
- 임계치 초과 시 `FRONT_OFFLOAD_ORIGIN`으로 offload 하려면 front VM에도 Public IP + `80/tcp` Inbound가 필요합니다.

## 2) VM별 env 파일 생성
루트 경로(`/Users/jaehyun/Documents/IdeaProjects/KaKaoCloud/Project/ForkFundingBoost`)에서:

```bash
cp deploy/oci/env/edge.env.example deploy/oci/env/edge.env
cp deploy/oci/env/front.env.example deploy/oci/env/front.env
cp deploy/oci/env/back.env.example deploy/oci/env/back.env
```

필수 수정:
- `deploy/oci/env/edge.env`
  - 기본값이 현재 IP로 반영됨 (`FRONT_PRIVATE_HOST=10.0.1.198`, `BACK_PRIVATE_HOST=10.0.1.62`, `BACK_PRIVATE_PORT=8080`)
  - Catalog 라우팅(Edge -> Catalog)
    - `CATALOG_PRIVATE_HOST=10.0.1.62`
    - `CATALOG_PRIVATE_PORT=8090`
    - `CATALOG_ROUTE_MODE=off|canary|full` (`off` 권장 시작)
    - `CATALOG_CANARY_PERCENT=5` (canary 모드 비율)
    - 강제 라우팅(운영 점검용): 헤더 `X-Catalog-Route: catalog|back` 또는 쿠키 `fb_catalog_route=catalog|back`
  - Payment 라우팅(Edge -> Payment)
    - `PAYMENT_PRIVATE_HOST=10.0.1.62`
    - `PAYMENT_PRIVATE_PORT=8081`
    - `PAYMENT_ROUTE_MODE=off|canary|full` (`off` 권장 시작)
    - `PAYMENT_CANARY_PERCENT=5` (canary 모드 비율)
    - 강제 라우팅(운영 점검용): 헤더 `X-Payment-Route: payment|back` 또는 쿠키 `fb_payment_route=payment|back`
  - 접근 제어/보안
    - `EDGE_ALLOWED_HOST_REGEX`: 허용 Host 헤더 정규식 (예: `138[.]2[.]43[.]7|fundingboost[.]example[.]com|localhost|127[.]0[.]0[.]1`)
    - `EDGE_ENTRY_PATH`: 최초 1회 접근용 게이트 URL 경로(랜덤 문자열 권장)
    - `EDGE_ACCESS_COOKIE_MAX_AGE`: 게이트 쿠키 유지 시간(초)
    - `EDGE_RATE_LIMIT_PER_IP`, `EDGE_RATE_LIMIT_BURST`, `EDGE_CONN_LIMIT`: IP당 요청률/버스트/동시연결 제한
  - 월 누적 트래픽 가드
    - `TRAFFIC_THRESHOLD_TB=9` (임계치)
    - `TRAFFIC_INTERFACE=` (비워두면 자동 감지)
    - `EDGE_CONTAINER_NAME=fundingboost-edge`
    - `FRONT_OFFLOAD_ORIGIN=` (예: `http://<FRONT_PUBLIC_IP>`, 설정 시 임계치 도달 시 정적/이미지 요청을 front로 302 offload)
- `deploy/oci/env/back.env`
  - `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`
  - `CRAWLER_DB_NAME=item` (상품 카탈로그 전용 DB)
  - `PAYMENT_DB_NAME=payment` (결제 전용 DB, 모의 MSA 스키마 분리)
  - `FUNDINGBOOST_DB_URL`, `CRAWLER_DB_URL` (`mysql` 호스트 기준)
  - `FUNDINGBOOST_DB_USERNAME`, `FUNDINGBOOST_DB_PASSWORD`, `CRAWLER_DB_USERNAME`, `CRAWLER_DB_PASSWORD`
  - `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`
  - `KAKAO_REDIRECT_URI=http://<EDGE_PUBLIC_HOST>/login/oauth2/code/kakao`
  - `APP_PAY_BARCODE_VERIFY_BASE_URL=http://<EDGE_PUBLIC_HOST>` (QRbot 스캔 시 열리는 바코드 검증 URL 호스트)
  - Catalog 점진 전환 플래그
    - `APP_CATALOG_READER=local|remote`
    - `APP_CATALOG_REMOTE_BASE_URL=http://funding-crawler:8090`
    - `APP_CATALOG_REMOTE_FALLBACK_TO_LOCAL=true|false`
  - Payment 1차 이관/후속 처리 플래그
    - `APP_PAYMENT_PROXY_BASE_URL=http://fundingboost-server:8080`
    - `APP_PAYMENT_FOLLOW_UP_MODE=direct|consumer`
    - `APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED=true|false`
    - `APP_PAYMENT_TOPICS_COMPLETED=fundingboost.payment.completed.v1`
    - `APP_PAYMENT_TOPICS_FAILED=fundingboost.payment.failed.v1`
    - 현재 운영 기준: `APP_PAYMENT_FOLLOW_UP_MODE=consumer`, `APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED=true`
  - 관리자 페이지 접근 계정은 서버 `member_role=ROLE_ADMIN` 사용자로 제어됨 (기본 QA 계정은 `qa1@fundingboost.test`)
  - `FUNDINGBOOST_CORS_ORIGINS=http://<EDGE_PUBLIC_HOST>`

참고:
- MySQL은 `schema == database` 개념입니다.
- 이 구성에서는 `fundingboost`(서버 도메인), `item`(상품 카탈로그), `payment`(결제 모듈) DB로 분리합니다.

## 2-1) MSA 마이그레이션 순서 (이유 포함)
1. 모놀리스 내부 경계 고정 (현재 단계)
   - 적용: Outbox + Kafka 이벤트 발행, Catalog ACL 포트 도입
   - 이유: 서비스 분리 전에 의존성 방향을 고정해야 분리 비용이 급격히 줄어듭니다.
2. Catalog 서비스 분리 (`/api/v1/items`, `/api/v3/*`)
   - 이유: 조회 트래픽 비중이 높아 가장 먼저 분리했을 때 성능/캐시 효과가 큽니다.
3. Payment 서비스 분리 (`payment` 스키마 소유권 이전)
   - 이유: 이미 결제 전용 스키마와 오케스트레이션이 있어 추출 난이도가 낮습니다.
4. Funding/Commerce 서비스 분리 (Saga + 이벤트 기반 상태 전파)
   - 이유: 교차 트랜잭션을 제거해 장애 전파와 배포 결합도를 낮춥니다.
5. Admin/Home 읽기 모델 분리 (Projection)
   - 이유: 조인성 조회를 이벤트 집계 테이블로 전환해 조회 성능과 독립 배포성을 높입니다.

### Catalog 점진 전환 순서
1. 초기값 고정
   - Back: `APP_CATALOG_READER=local`
   - Edge: `CATALOG_ROUTE_MODE=off`
2. 원격 조회 준비
   - Back: `APP_CATALOG_READER=remote`, `APP_CATALOG_REMOTE_FALLBACK_TO_LOCAL=true`
   - Catalog(crawler) API 헬스/응답 스펙 점검
3. 카나리 시작
   - Edge: `CATALOG_ROUTE_MODE=canary`, `CATALOG_CANARY_PERCENT=5`
4. 점진 확대
   - `CATALOG_CANARY_PERCENT=25 -> 50 -> 100`
5. 전체 전환
   - Edge: `CATALOG_ROUTE_MODE=full`
   - 안정화 확인 후 Back: `APP_CATALOG_REMOTE_FALLBACK_TO_LOCAL=false`
6. 장애 시 롤백
   - Edge: `CATALOG_ROUTE_MODE=off` 또는 헤더/쿠키로 `back` 강제
   - Back: 필요 시 `APP_CATALOG_READER=local` 복귀

### Payment 점진 전환 순서
1. 초기값 고정
   - Edge: `PAYMENT_ROUTE_MODE=off`
   - Back: `APP_PAYMENT_FOLLOW_UP_MODE=direct`
2. Payment 1차 이관 배포
   - Payment: `/api/v1/pay/**` proxy 어댑터 배포
   - Back: `payment.completed.v1`, `payment.failed.v1` 발행 코드 배포
3. 카나리 시작
   - Edge: `PAYMENT_ROUTE_MODE=canary`, `PAYMENT_CANARY_PERCENT=5`
   - 검증: QA 계정으로 `X-Payment-Route: payment` 강제 호출 후 응답 동일성 확인
4. 후속 처리 consumer-mode 전환 준비
   - Back: `APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED=true`
   - Back: `APP_PAYMENT_FOLLOW_UP_MODE=consumer`
   - 이유: `payment.completed.v1`를 Commerce/Funding이 구독하는 구조로 전환
5. 카나리 확대
   - `PAYMENT_CANARY_PERCENT=25 -> 50 -> 100`
   - 2026-03-24 검증 완료: `PAYMENT_CANARY_PERCENT=100`
   - native 실행 범위: `POST /api/v1/pay/order`, `POST /api/v1/pay/order/now`, `POST /api/v1/pay/funding/{fundingItemId}`
   - `POST /api/v1/pay/friends/*`는 Payment 서비스 내부에서 contributor/barcode를 직접 저장하도록 이전 완료
6. 전체 전환
   - Edge: `PAYMENT_ROUTE_MODE=full`
   - 현재 운영 기준(2026-03-25): `PAYMENT_ROUTE_MODE=full`, `PAYMENT_CANARY_PERCENT=100`
   - 추가 반영(2026-03-25):
     - `PaymentOrderFinalizeService`로 `order`, `order/now`, `funding/*` finalize를 Payment 서비스 내부 실행으로 이동
     - native 오류 응답에도 `X-Payment-Service-Mode` 헤더 보장
     - `payment.completed.v1` 후속 변환을 도메인별 dispatcher/service로 분리
     - `funding.contribution.created.v1` key를 `fundingId:contributorId`로 보정
   - 최종 완료 반영(2026-03-25):
     - Back: `APP_PAYMENT_FOLLOW_UP_MODE=direct`
     - Back: `APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED=false`
     - Payment가 `commerce.order.paid.v1`, `funding.contribution.created.v1`를 직접 outbox에 적재
     - 신규 QA 이벤트가 Payment outbox에만 생성되고 Back outbox에는 생성되지 않음을 확인
   - QA 검증: `qa1~qa5` invalid friend parity + 오류 응답 header 확인, `order/now` 성공, `order` idempotency 재호출 성공, `funding 1` remain pay 성공, `qa1 -> funding 2` direct friend 결제, `qa3 -> funding 4` barcode issue/consume, `payment.completed.v1` -> `commerce.order.paid.v1`/`funding.contribution.created.v1` Payment 직접 발행 확인
   - 참고: 오래된 QA funding `1/2/4`의 stale `item_id`를 현재 Catalog item(`1974/1973/1972`)로 보정해 성공 경로를 재검증함
7. 장애 시 롤백
   - Edge: `PAYMENT_ROUTE_MODE=off`
   - Back: `APP_PAYMENT_FOLLOW_UP_MODE=direct`

## 3) 로컬에서 ARM 이미지 빌드 + tar 생성
M 시리즈 맥 기준:

```bash
./deploy/oci/scripts/build-arm-images.sh
```

결과물:
- `deploy/oci/artifacts/fundingboost-front_arm64.tar.gz`
- `deploy/oci/artifacts/fundingboost-server_arm64.tar.gz`
- `deploy/oci/artifacts/fundingboost-crawler_arm64.tar.gz`
- `deploy/oci/artifacts/fundingboost-payment_arm64.tar.gz` (`FundingBoost-Payment` 개별 빌드 시)

## 4) VM으로 파일 전송
각 VM에 최소 아래 파일을 전송:
- 공통: `docker-compose.edge.yml`, `docker-compose.front.yml`, `docker-compose.back.yml`, `deploy/oci/**`
- Front VM: `fundingboost-front_arm64.tar.gz`
- Back VM: `fundingboost-server_arm64.tar.gz`, `fundingboost-crawler_arm64.tar.gz`, `fundingboost-payment_arm64.tar.gz`

## 5) Front VM 실행
```bash
docker load -i fundingboost-front_arm64.tar.gz
docker compose \
  --env-file deploy/oci/env/front.env \
  -f docker-compose.front.yml up -d
```

## 6) Back VM 실행
Elasticsearch 1회 커널 설정:
```bash
sudo sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" | sudo tee /etc/sysctl.d/99-elasticsearch.conf
sudo sysctl --system
```

컨테이너 실행:
```bash
docker load -i fundingboost-server_arm64.tar.gz
docker load -i fundingboost-crawler_arm64.tar.gz
docker load -i fundingboost-payment_arm64.tar.gz
docker compose \
  --env-file deploy/oci/env/back.env \
  -f docker-compose.back.yml up -d
```

참고:
- Kafka 이미지는 현재 `apache/kafka:3.7.2` 기준입니다.
- `OutboxRelayService`는 개별 이벤트를 별도 트랜잭션으로 relay하도록 보정되어 있어야 합니다.

## 7) Edge VM 실행
```bash
docker compose \
  --env-file deploy/oci/env/edge.env \
  -f docker-compose.edge.yml up -d
```

트래픽 가드 크론 설치(권장):
```bash
bash deploy/oci/edge/scripts/install-traffic-guard-cron.sh
```

## 8) 월 누적 트래픽 가드 동작
- 감시 대상: Edge VM 네트워크 인터페이스 `tx_bytes` 누적(월 단위)
- 임계치 미만: `mode=normal`
  - `Nginx`가 기존처럼 front/back으로 reverse proxy 유지
- 임계치 이상 + `FRONT_OFFLOAD_ORIGIN` 설정됨: `mode=offload`
  - `/api/*`는 계속 edge -> back 유지
  - `/static/*`, `/img-proxy*`, `asset-manifest.json`, `manifest.json`, `favicon.ico`는 `302`로 front public origin에 offload
- 임계치 이상 + `FRONT_OFFLOAD_ORIGIN` 미설정: `mode=throttle`
  - `/api/*`와 일반 페이지는 유지
  - `/img-proxy*`만 `503`으로 차단(비필수 기능 축소)
- 월이 바뀌면 자동으로 `mode=normal` 복귀하고 누적치 초기화

상태 조회:
- 브라우저/CLI에서 `http://<EDGE_PUBLIC_HOST>/_edge/traffic-status`

## 8-1) 게이트 URL 기반 접근 제어
- Edge는 `EDGE_ALLOWED_HOST_REGEX`에 맞는 Host만 응답합니다(그 외 `444`).
- 사용자 트래픽은 게이트 URL을 먼저 1회 호출해야 접근 가능합니다.
  - 예: `http://<EDGE_PUBLIC_HOST>/<EDGE_ENTRY_PATH>`
  - 호출 시 `fb_gate` 쿠키를 발급하고 `/`로 302 이동합니다.
- 쿠키가 없으면 일반 페이지/API 요청은 `404`로 차단됩니다.
- 이 방식은 단순 context-path 노출보다 안전하며, 무작위 스캔 트래픽과 불필요 egress를 줄이는 데 효과적입니다.

## 9) 점검
Front VM:
```bash
docker compose -f docker-compose.front.yml ps
```

Back VM:
```bash
docker compose -f docker-compose.back.yml ps
docker compose -f docker-compose.back.yml logs -f fundingboost-server
```

Edge VM:
```bash
docker compose -f docker-compose.edge.yml ps
docker compose -f docker-compose.edge.yml logs -f fundingboost-edge
```

## 10) 현재 포트 구조
- `Edge VM`
  - `80`: 외부 공개 진입점
- `Front VM`
  - `80`: Edge만 접근
- `Back VM`
  - `8080`: `fundingboost-server` 공개 포트 (Private IP로만 접근)
  - `8090`: `funding-crawler` 공개 포트 (Private IP로만 접근, 필요 시만 사용)
  - `9092`: Kafka 브로커 (내부 네트워크)
  - `6379`: Redis(내부)
  - `9200`: Elasticsearch(내부)

## 11) 참고
- `docker-compose.local.yml`는 로컬 단일 VM 통합 테스트용 파일(MySQL 포함)로 유지됨.
- 3-VM 운영은 `docker-compose.edge.yml`, `docker-compose.front.yml`, `docker-compose.back.yml`를 사용.

## 12) 로컬 Docker 통합 실행 (개발 점검용)
루트의 `.env`를 확인한 뒤:

```bash
docker compose -f docker-compose.local.yml up -d --build
```

주요 엔드포인트:
- Front+Edge 단일 진입: `http://localhost`
- API: `http://localhost/api/v1/*`

QRbot(iPhone)로 로컬 바코드 테스트 시:
- `APP_PAY_BARCODE_VERIFY_BASE_URL`를 Mac의 같은 Wi-Fi 대역 IP로 설정  
  예) `http://192.168.0.12`
- 설정 후 `docker compose -f docker-compose.local.yml up -d --build --no-deps fundingboost-server` 재기동

종료:
```bash
docker compose -f docker-compose.local.yml down
```
