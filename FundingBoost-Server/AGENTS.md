# Server Guidelines

## Scope
- This file applies to everything under `FundingBoost-Server/`.
- This is the main Spring Boot service for the FundingBoost domain.

## Structure
- Main packages are under `src/main/java/kcs/funding/fundingboost`.
- `catalog/`: local or remote catalog reader integration.
- `domain/`: controllers, services, entities, repositories, security, and core business logic.
- `elasticsearch/`: indexing and search-related integration.
- `docs/`: server-specific documentation such as barcode and QRbot flow notes.

## Working Rules
- Use the committed Gradle wrapper for commands in this module.
- Keep changes package-local when possible; do not move broad slices of the monolith unless the task explicitly asks for it.
- Preserve current Spring configuration and environment variable names used by Docker Compose and deployment files.
- If you change admin, barcode, or QRbot-related behavior, check whether `docs/QRBOT_CODE128_FLOW.md` also needs an update.
- Do not commit generated build output.

## QueryDSL Note
- This module uses generated QueryDSL sources via `src/main/generated`.
- If you touch JPA entities, repositories, or QueryDSL configuration, verify generated `Q*` type assumptions before changing unrelated code.
- Do not “fix” build issues by committing generated sources unless the task explicitly asks for that approach.

## Commands
- Test: `./gradlew test`
- Compile: `./gradlew compileJava`
- Run locally: `./gradlew bootRun`
- Build: `./gradlew build`

## Validation
- Prefer the smallest relevant Gradle task first, such as `test` or `compileJava`.
- If your change affects generated docs or Spring REST Docs flow, validate the related test/docs task rather than only a generic build.
- Be aware that the current branch may already have QueryDSL generation-related compile failures; do not spend time fixing them unless requested.
