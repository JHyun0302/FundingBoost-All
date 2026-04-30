PROFILE_ID=stage3-payment-cutover
PROFILE_NAME=3단계 Payment 서비스 분리
PROFILE_REASON=결제 분리는 외부 진입점, 내부 실행 ownership, 후속 이벤트 계약이 함께 고정되어야 한다.
PASS_CONDITION=server/payment 컴파일, edge/back 설정 유효성, Payment 전환 플래그와 후속 처리 플래그 문서화가 모두 통과한다.
REQUIRED_FILE::FundingBoost-Server/src/main/java/kcs/funding/fundingboost/payment/application/PaymentFacade.java|결제 진입 경계가 facade로 고정되어야 한다.
REQUIRED_FILE::FundingBoost-Payment/src/main/java/kcs/funding/payment/api/PaymentWriteController.java|Payment 서비스가 외부 결제 쓰기 요청을 받아야 한다.
REQUIRED_FILE::FundingBoost-Payment/src/main/java/kcs/funding/payment/order/application/PaymentOrderFinalizeService.java|최종 주문 생성 책임이 Payment 내부로 이동해야 한다.
REQUIRED_FILE::deploy/oci/env/edge.env.example|Payment edge 전환 플래그 예시가 있어야 한다.
CHECK::server_compile|||cd FundingBoost-Server && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q compileJava|||Back 서버가 Payment 경계와 함께 컴파일되어야 한다.
CHECK::payment_compile|||cd FundingBoost-Payment && JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew -q compileJava|||Payment 서비스가 wrapper 기준으로 독립 컴파일되어야 한다.
CHECK::back_compose_config|||docker compose -f docker-compose.back.yml config >/dev/null|||Back compose 구성이 Payment 서비스 추가 이후에도 유효해야 한다.
CHECK::edge_compose_config|||docker compose -f docker-compose.edge.yml config >/dev/null|||Edge compose 구성이 Payment 라우팅 플래그와 함께 유효해야 한다.
CHECK::payment_route_flags|||rg -n "PAYMENT_ROUTE_MODE|PAYMENT_CANARY_PERCENT|APP_PAYMENT_FOLLOW_UP_MODE|APP_PAYMENT_FOLLOW_UP_CONSUMERS_ENABLED" deploy/oci/env/edge.env.example deploy/oci/env/back.env.example docs/MSA_MIGRATION_EXECUTION_ORDER.md >/dev/null|||Payment 전환 플래그와 후속 처리 플래그가 문서와 예시에 고정되어야 한다.
