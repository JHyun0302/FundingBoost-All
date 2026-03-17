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
  - 관리자 페이지 접근 계정은 서버 `member_role=ROLE_ADMIN` 사용자로 제어됨 (기본 QA 계정은 `qa1@fundingboost.test`)
  - `FUNDINGBOOST_CORS_ORIGINS=http://<EDGE_PUBLIC_HOST>`

참고:
- MySQL은 `schema == database` 개념입니다.
- 이 구성에서는 `fundingboost`(서버 도메인), `item`(상품 카탈로그), `payment`(결제 모듈) DB로 분리합니다.

## 3) 로컬에서 ARM 이미지 빌드 + tar 생성
M 시리즈 맥 기준:

```bash
./deploy/oci/scripts/build-arm-images.sh
```

결과물:
- `deploy/oci/artifacts/fundingboost-front_arm64.tar.gz`
- `deploy/oci/artifacts/fundingboost-server_arm64.tar.gz`
- `deploy/oci/artifacts/fundingboost-crawler_arm64.tar.gz`

## 4) VM으로 파일 전송
각 VM에 최소 아래 파일을 전송:
- 공통: `docker-compose.edge.yml`, `docker-compose.front.yml`, `docker-compose.back.yml`, `deploy/oci/**`
- Front VM: `fundingboost-front_arm64.tar.gz`
- Back VM: `fundingboost-server_arm64.tar.gz`, `fundingboost-crawler_arm64.tar.gz`

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
docker compose \
  --env-file deploy/oci/env/back.env \
  -f docker-compose.back.yml up -d
```

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
