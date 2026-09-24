# TASK-008 — rename account-service to auth-service

## Status: COMPLETED (2026-09-24)

Category: Backend / platform. Operator request 2026-09-24: full rename.
Earlier TASK-*/REVIEW-*/RELEASE-* records keep the name they were written under.

## Changes

- `backend/account-service` → `backend/auth-service`; Maven artifact, module,
  `spring.application.name`, Dockerfile jar, CI image `buildingos/auth-service`.
- Java package `com.buildingos.account` → `com.buildingos.auth` (feature `auth` is now
  `com.buildingos.auth.auth`); `AccountApplication` → `AuthApplication`;
  ArchUnit rules in building/subscription services updated to `com.buildingos.auth..`.
- Database `account_db`/`account_app` → `auth_db`/`auth_app`; init script `01-auth-db.sh`.
- Env: `ACCOUNT_DB_PASSWORD` → `AUTH_DB_PASSWORD`, `ACCOUNT_SERVICE_URL` →
  `AUTH_SERVICE_URL`, `ACCOUNT_SIGNING_JWKS_PATH`/`ACCOUNT_SIGNING_ACTIVE_KID`/
  `ACCOUNT_ACCESS_TOKEN_TTL` → `AUTH_*`; property prefix `buildingos.account.*` → `buildingos.auth.*`.
- Gateway metadata route `/api/v1/platform/account` → `/api/v1/platform/auth`
  (OpenAPI updated). OTP routes `/api/v1/auth/otp/*` unchanged.
- Docs: ARCHITECTURE, LOCAL_DEVELOPMENT, AUTH_CONFIGURATION.

## Operator follow-up

Existing local Postgres volumes still hold `account_db`; init scripts run only on a fresh
volume. Rename `ACCOUNT_DB_PASSWORD` to `AUTH_DB_PASSWORD` in `infra/docker/.env`, then
`docker compose -f infra/docker/compose.yaml down -v` and re-run `scripts/verify-platform.sh`.

## Evidence

`mvn -B -f backend/pom.xml clean verify`: BUILD SUCCESS, 147 tests, 0 failures/errors.
`scripts/check-contracts.py`: passed.
