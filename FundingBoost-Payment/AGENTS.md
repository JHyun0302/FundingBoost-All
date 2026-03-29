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
- Do not add a Gradle wrapper unless the task explicitly asks for it.
- Do not commit generated build output.

## Commands
- Install/resolve dependencies with the local Gradle installation in this directory.
- Test: `gradle test`
- Run locally: `gradle bootRun`
- Build: `gradle build`

## Validation
- Validate the smallest relevant Gradle task for the code you changed.
- Be aware that local validation can currently fail depending on the local Gradle/JDK combination because this module does not commit a wrapper.
- If the task spans payment events consumed by another service, note that cross-service validation may be needed separately.
