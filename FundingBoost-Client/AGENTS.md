# Client Guidelines

## Scope
- This file applies to everything under `FundingBoost-Client/`.
- The actual React application lives in `FundingBoost-Client/fundingboost`.
- Top-level Docker and nginx files in this module are deployment assets for the client bundle.

## Structure
- App entry points: `fundingboost/src/index.js`, `fundingboost/src/App.jsx`, `fundingboost/src/router.jsx`.
- API helpers: `fundingboost/src/api/`.
- Shared state and utilities: `fundingboost/src/state/`, `fundingboost/src/utils/`.
- UI follows the existing `atoms` / `molecules` / `organisms` / `pages` folder split under `fundingboost/src/components`.

## Working Rules
- Run Node commands inside `FundingBoost-Client/fundingboost`, not from the repo root.
- Reuse the existing router, API utilities, and shared components before adding new top-level patterns.
- Keep CSS naming in BEM style, matching the existing client README guidance.
- Do not edit generated build output or `node_modules`.
- Be careful with asset moves or renames because many pages import images directly from `src/assets`.

## Commands
- Install: `cd fundingboost && npm install`
- Dev server: `cd fundingboost && npm start`
- Test: `cd fundingboost && npm test`
- Production build: `cd fundingboost && npm run build`

## API and Config
- Prefer existing axios setup in `fundingboost/src/api/axiosInstance.jsx`.
- Preserve `REACT_APP_*` environment variable names already used by the Docker and compose setup.
- If you change a route or request shape, update both the page/component caller and the shared API utility in the same task when appropriate.

## Validation
- Frontend-only change: run `npm test` or `npm run build` in `fundingboost`.
- Docker/nginx change: validate with the relevant `docker compose ... config` or targeted Docker build command.
