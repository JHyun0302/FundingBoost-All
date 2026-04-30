PROFILE_ID=stage1-monolith-boundary
PROFILE_NAME=1단계 모놀리스 내부 경계 고정
PROFILE_REASON=서비스 분리 전에 이벤트와 포트 경계를 먼저 고정해야 이후 분리 비용이 줄어든다.
PASS_CONDITION=서버 컴파일, back 배포 설정, 핵심 경계 파일 존재, 기준 문서의 1단계 정의가 모두 통과한다.
REQUIRED_FILE::FundingBoost-Server/src/main/java/kcs/funding/fundingboost/event/application/OutboxRelayService.java|Outbox relay가 있어야 이벤트 경계를 고정할 수 있다.
REQUIRED_FILE::FundingBoost-Server/src/main/java/kcs/funding/fundingboost/catalog/application/CatalogItemReader.java|Catalog ACL 포트가 있어야 조회 의존을 바꿀 수 있다.
REQUIRED_FILE::deploy/oci/env/back.env.example|운영 플래그 기준 파일이 있어야 같은 설정으로 검증할 수 있다.
CHECK::server_compile|||cd FundingBoost-Server && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q compileJava|||경계 변경 이후 서버가 컴파일되어야 한다.
CHECK::back_compose_config|||docker compose -f docker-compose.back.yml config >/dev/null|||back 배포 구성이 깨지지 않아야 한다.
CHECK::migration_checklist|||rg -n "1단계: 모놀리스 내부 경계 고정" docs/MSA_MIGRATION_EXECUTION_ORDER.md >/dev/null|||1단계 통과 기준이 문서에 있어야 한다.
CHECK::event_contract_docs|||rg -n "funding\.contribution\.created\.v1|commerce\.order\.paid\.v1" docs/MSA_MIGRATION_EXECUTION_ORDER.md docs/MSA_MIGRATION_KAFKA_PLAYBOOK.md >/dev/null|||핵심 이벤트 계약이 기준 문서에 있어야 한다.
