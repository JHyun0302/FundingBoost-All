# DataCrawler Guidelines

## Scope
- This file applies to everything under `FundingBoost-DataCrawler/`.
- This module combines catalog API responsibilities with item crawling/bootstrap jobs.

## Structure
- Code lives under `src/main/java/kcs/funding/crawler`.
- `catalog/`: externally consumed catalog DTOs, controllers, and read services.
- `service/`: crawling, brand discovery, and item synchronization logic.
- `Runner/`: bootstrap and scheduled execution entry points.
- `config/`: scheduler, webdriver, and application configuration.
- `entity/` and `repository/`: crawler-side persistence.

## Working Rules
- Use the committed Gradle wrapper for commands in this module.
- Keep catalog API changes backward-compatible with the main server's remote catalog client unless the task explicitly coordinates both sides.
- Isolate crawling/scheduler changes from catalog API work when possible.
- Be careful with WebDriver and scheduler-related settings because they affect startup and long-running jobs.
- Do not commit generated build output or generated WAR files.

## Commands
- Test: `./gradlew test`
- Compile classes: `./gradlew testClasses`
- Run locally: `./gradlew bootRun`
- Build WAR: `./gradlew bootWar`

## Validation
- Catalog API change: run `./gradlew test` or the smallest relevant test task.
- Packaging/deployment change: run `./gradlew bootWar`.
- If you change the crawler bootstrap or scheduler flow, validate startup assumptions separately from pure API tests.
