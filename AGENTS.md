# Repository Guidelines

## Overview
- This repository is a multi-service FundingBoost workspace with a React client, Spring Boot services, and Docker-based local/infrastructure deployment files.
- Keep changes scoped to the module you are touching. Avoid broad refactors across services unless the task explicitly requires them.
- Do not edit generated artifacts or dependency directories such as `**/build`, `**/out`, `**/.gradle`, or `**/node_modules`.

## Repository Map
- `FundingBoost-Client/fundingboost`: React 18 client built with `react-scripts`.
- `FundingBoost-Server`: main Spring Boot service (Java 17) with MySQL, Redis, Elasticsearch, and Kafka integrations.
- `FundingBoost-Payment`: Spring Boot payment service (Java 17).
- `FundingBoost-DataCrawler`: Spring Boot crawler/catalog service (Java 17).
- `docker-compose.local.yml`: single-machine local integration stack.
- `docker-compose.back.yml`, `docker-compose.edge.yml`, `docker-compose.front.yml`: split deployment compose files.
- `deploy/`: OCI and Jenkins deployment assets.

## Working Rules
- Prefer module-local commands and validation instead of repo-wide commands.
- Preserve existing environment variable names and Docker service names when editing compose or deployment files.
- Never hard-code secrets; use `.env`, deployment env files, or existing Spring/React environment variable patterns.
- If an API contract changes, update the calling client, server docs, and any related compose or env wiring in the same task when needed.

## Setup and Common Commands

### Frontend
- Install dependencies in `FundingBoost-Client/fundingboost` with `npm install`.
- Common commands:
  - `npm start`
  - `npm test`
  - `npm run build`

### Server
- Use the Gradle wrapper in `FundingBoost-Server`.
- Common commands:
  - `./gradlew test`
  - `./gradlew bootRun`
  - `./gradlew build`

### Payment
- Use the committed Gradle wrapper in `FundingBoost-Payment`.
- Common commands:
  - `./gradlew test`
  - `./gradlew bootRun`
  - `./gradlew build`

### DataCrawler
- Use the Gradle wrapper in `FundingBoost-DataCrawler`.
- Common commands:
  - `./gradlew test`
  - `./gradlew bootRun`
  - `./gradlew build`

### Local Integration
- Use `docker compose -f docker-compose.local.yml up --build` for the combined local stack.
- When only checking configuration changes, prefer `docker compose -f docker-compose.local.yml config`.

## Code Style Notes

### Frontend
- Follow the existing component structure under `src/components` and related folders.
- Keep CSS class naming in BEM style, matching the existing client README guidance.
- Reuse existing API utilities, state modules, and shared components before adding new patterns.

### Backend Services
- Keep Java compatibility at 17 unless the task explicitly changes the toolchain.
- Match existing Spring Boot conventions, package structure, and test style already used in each service.
- Prefer focused service-layer or controller-layer changes over broad package moves.

## Validation Expectations
- Validate the smallest relevant scope first:
  - frontend-only change: run client tests or build in `FundingBoost-Client/fundingboost`
  - single-service backend change: run that service's Gradle tests
  - compose/deploy change: run `docker compose ... config` at minimum
- If a change spans multiple services, validate each touched service separately rather than relying on one broad command.
- For current MSA migration work, prefer the harness profiles in `./scripts/harness/run.sh` so the same task type always runs the same checks.

## Safety
- Treat `.env`, deployment env files, SSH-related material, and cloud deployment settings as sensitive.
- Do not commit generated archives, logs, local backups, or dependency directories.
