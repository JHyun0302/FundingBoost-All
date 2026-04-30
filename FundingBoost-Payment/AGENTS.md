# Payment Service Guidelines

## Scope
- This file applies to everything under `FundingBoost-Payment/`.
- This is the Spring Boot payment service extracted from the main system.

## Structure
- Code lives under `src/main/java/kcs/funding/payment`.
- `api/`: externally exposed payment endpoints and DTO wiring.
- `friend/` and `nativeflow/`: friend-funding and native payment flows.
- `order/`: payment order and delivery persistence.
- `payment/`: payment intent, gateway, and orchestration logic.
- `consumer/` and `event/`: Kafka/event-driven integration.

## Working Rules
- Keep payment flow changes cohesive across controller, application, domain, and persistence layers.
- Preserve idempotency and intent-state behavior when touching payment orchestration code.
- Keep external request/response contracts compatible with the client and main server unless the task explicitly includes coordinated contract changes.
- Do not commit generated build output.

## Commands
- Use the committed Gradle wrapper in this directory.
- Test: `./gradlew test`
- Run locally: `./gradlew bootRun`
- Build: `./gradlew build`

## Validation
- Validate the smallest relevant Gradle task for the code you changed.
- For payment migration work, prefer `./scripts/harness/run.sh stage3-payment-cutover` before broader manual QA.
- If the task spans payment events consumed by another service, note that cross-service validation may be needed separately.
