PROFILE_ID=stage2-catalog-cutover
PROFILE_NAME=2단계 Catalog 서비스 분리
PROFILE_REASON=조회 트래픽 전환은 경로 분기와 응답 호환성이 먼저 고정되어야 안전하다.
PASS_CONDITION=server/crawler 컴파일, edge 라우팅 설정 유효성, Catalog 플래그 문서화가 모두 통과한다.
REQUIRED_FILE::FundingBoost-Server/src/main/java/kcs/funding/fundingboost/catalog/infra/RemoteCatalogItemReader.java|Back 서버 원격 Catalog 어댑터가 있어야 한다.
REQUIRED_FILE::FundingBoost-DataCrawler/src/main/java/kcs/funding/crawler/catalog/controller/CatalogV3Controller.java|Catalog 서비스 API가 있어야 한다.
REQUIRED_FILE::FundingBoost-Server/src/main/resources/application-catalog.yml|Catalog feature flag 기준 설정이 있어야 한다.
REQUIRED_FILE::deploy/oci/edge/nginx.conf.template|Edge 라우팅 규칙이 있어야 한다.
CHECK::server_compile|||cd FundingBoost-Server && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q compileJava|||Back 서버가 원격 Catalog 경계와 함께 컴파일되어야 한다.
CHECK::crawler_compile|||cd FundingBoost-DataCrawler && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q compileJava|||Catalog 서비스가 독립 컴파일되어야 한다.
CHECK::edge_compose_config|||docker compose -f docker-compose.edge.yml config >/dev/null|||Edge compose 구성이 유효해야 한다.
CHECK::catalog_route_flags|||rg -n "CATALOG_ROUTE_MODE|CATALOG_CANARY_PERCENT|APP_CATALOG_READER|APP_CATALOG_REMOTE_FALLBACK_TO_LOCAL" deploy/oci/env/edge.env.example deploy/oci/env/back.env.example docs/MSA_MIGRATION_EXECUTION_ORDER.md >/dev/null|||Catalog 전환 플래그가 예시와 문서에 고정되어야 한다.
