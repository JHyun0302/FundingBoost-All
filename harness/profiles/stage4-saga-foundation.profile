PROFILE_ID=stage4-saga-foundation
PROFILE_NAME=4단계 Funding·Commerce Saga 기반
PROFILE_REASON=상태 전파를 이벤트 소비로 옮기려면 inbox와 idempotent consumer 기반이 먼저 있어야 한다.
PASS_CONDITION=서버 컴파일, back 설정 유효성, saga inbox 파일 존재, inbox 플래그와 토픽 정의 문서화가 모두 통과한다.
REQUIRED_FILE::FundingBoost-Server/src/main/java/kcs/funding/fundingboost/saga/consumer/DomainSagaConsumer.java|도메인 saga consumer가 있어야 한다.
REQUIRED_FILE::FundingBoost-Server/src/main/java/kcs/funding/fundingboost/saga/inbox/InboxEventService.java|중복 소비 방지용 inbox 서비스가 있어야 한다.
REQUIRED_FILE::FundingBoost-Server/src/main/resources/application-kafka.yml|Saga 토픽 설정이 리포에 고정되어야 한다.
REQUIRED_FILE::deploy/oci/env/back.env.example|inbox consumer 운영 스위치 예시가 있어야 한다.
CHECK::server_compile|||cd FundingBoost-Server && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q compileJava|||Saga 기반 추가 이후 서버가 컴파일되어야 한다.
CHECK::back_compose_config|||docker compose -f docker-compose.back.yml config >/dev/null|||Back compose 구성이 유지되어야 한다.
CHECK::inbox_flags_documented|||rg -n "APP_INBOX_CONSUMERS_ENABLED" deploy/oci/env/back.env.example docs/MSA_MIGRATION_EXECUTION_ORDER.md docs/MSA_MIGRATION_KAFKA_PLAYBOOK.md >/dev/null|||inbox consumer 스위치가 문서와 예시에 있어야 한다.
CHECK::saga_topics_documented|||rg -n "commerce\.order\.paid\.v1|funding\.contribution\.created\.v1|payment\.failed\.v1" FundingBoost-Server/src/main/resources/application-kafka.yml docs/MSA_MIGRATION_KAFKA_PLAYBOOK.md >/dev/null|||Saga 토픽이 설정과 운영 문서에 동시에 있어야 한다.
