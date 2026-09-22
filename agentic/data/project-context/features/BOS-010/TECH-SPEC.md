# TECH-SPEC-BOS-010 — Identity & platform roles (first slice only)

## Status

Covers TASK-001 only: global user identity, platform roles, first-`SUPER_ADMIN`
bootstrap, phone+OTP login. Does **not** cover building application/lifecycle,
units/ownership, subscription/entitlements, or the back-office console — those are
separate, un-started features (see TASKS.md). D-09 (back-office web tech) and D-10
(support/onboarding scope enumeration) are deferred to those later tasks; nothing here
depends on them.

## Discovered architecture gap (resolved for this slice)

BOS-001's `identity-service`/`api-gateway`/`building-service` only ever *validated*
JWTs against an operator-supplied external issuer (`JWT_ISSUER`/`JWT_JWK_SET_URI`
env vars; see `docs/LOCAL_DEVELOPMENT.md`). Nothing in the codebase issues tokens.
Phone+OTP login requires *issuing* a session token, which didn't exist. Local/test
only, following the same pattern LOCAL_DEVELOPMENT.md already anticipated ("use your
own HTTP-only local issuer... rejected outside local/test profiles"): `identity-service`
now also acts as its own local RSA-signed token issuer + JWKS publisher, gated to the
`local`/`test` Spring profiles. Production still requires an external, HTTPS issuer —
this local issuer is a development convenience, not a production provisioning path;
that remains explicit follow-up work, not invented here.

## Requirement-to-component mapping

ID-01 (phone+OTP) → `identity-service` OTP challenge use cases + `DevelopmentOtpCodeVerifier`.
ID-03 (session issuance) → `LocalRsaJwtIssuer` + `JwkSetController`.
ID-04/ID-05 (platform roles, SUPER_ADMIN bootstrap) → `PlatformRole`/`platform_user_role`
table + `SuperAdminSeeder`.

## New code (identity-service)

Package `com.buildingos.identity.auth`, Clean Architecture / ports-adapters, mirroring
the existing `platform` package's layering:

- `domain`: `User`, `PlatformRole` (enum: `SUPER_ADMIN, PLATFORM_ADMIN,
  ONBOARDING_AGENT, SUPPORT_AGENT, SUBSCRIPTION_ADMIN` — BRD §4.1), `OtpChallenge`.
- `application/port/in`: `StartOtpChallenge`, `VerifyOtpChallenge`.
- `application/port/out`: `OtpChallengeRepository`, `UserRepository`, `OtpCodeVerifier`
  (replaceable verification boundary — dev fixed-code today, a real SMS/OTP vendor
  adapter later, per the superseded BOS-002 `OTP-PROVIDER.md` contract), `TokenIssuer`.
- `application/usecase`: `StartOtpChallengeService`, `VerifyOtpChallengeService` — the
  OTP lifecycle contract (attempt binding, expiry, attempt limit of 5, single-use,
  atomic consumption) matches `OTP-PROVIDER.md` exactly.
- `infrastructure/persistence`: `JdbcOtpChallengeRepository`, `JdbcUserRepository`
  (plain `JdbcTemplate`, no JPA/Hibernate — this module only has `spring-boot-starter-jdbc`).
- `infrastructure/security`: `DevelopmentOtpCodeVerifier` (fixed code `000000`),
  `LocalRsaJwtIssuer` (in-process RSA keypair, signs access tokens with
  `platform_roles` claim), `JwkSetController` (`GET /.well-known/jwks.json`) — all
  `@Profile({"local","test"})`.
- `infrastructure/seed`: `SuperAdminSeeder` — idempotent `ApplicationRunner`, seed
  phone `01306999005`, catches `DataAccessException` so a database outage at startup
  degrades to a logged warning, never aborts the process (matches the existing
  "database outage fails readiness, not liveness" principle from BOS-001).
- `infrastructure/AuthConfiguration`: wires the use cases; `@Profile({"local","test"})`
  as a whole, since it depends on profile-gated `OtpCodeVerifier`/`TokenIssuer` beans
  and there is no production implementation of either yet.
- `presentation/rest/AuthController`: `POST /api/v1/auth/otp/start`,
  `POST /api/v1/auth/otp/verify`. `@Profile({"local","test"})`.

Migration: `db/migration/V2__auth_identity.sql` — `app_user`, `platform_user_role`,
`otp_challenge` tables in `identity_db`.

## Shared platform-web changes

- `SecuritySettings` gained `publicPaths` (`platform.security.public-paths`, defaults
  empty — no behavior change for services that don't set it). The security filter
  chain now `permitAll()`s these paths in addition to `/actuator/health`; every other
  request still requires a valid bearer token. `identity-service`'s own
  `application.yaml` sets it to the OTP start/verify endpoints plus the JWKS endpoint.
  `api-gateway`'s `application.yaml` sets it to the OTP endpoints (proxied through).
- CORS now allows `POST` and the `Content-Type` header (previously `GET`/`OPTIONS`
  and `Authorization` only) — needed for a JSON POST login request.
- `PlatformErrorHandler` gained: a 400 handler for `IllegalArgumentException`/
  `MethodArgumentNotValidException`, and a catch-all `Exception` → 500 handler. The
  catch-all fixes a real, general bug this work uncovered: an unhandled exception was
  falling through to the servlet container's default `/error` dispatch, which the
  security filter chain then rejected as unauthenticated — silently turning every
  unexpected 500 into a misleading 401. This affects all three services, not just the
  new OTP code.

## Gateway

`GatewayRoutes` proxies `POST /api/v1/auth/otp/start` and `/verify` to
`IDENTITY_SERVICE_URL`, same pattern as the existing metadata routes (no path rewrite
needed — same path on both sides).

## Explicitly not done in this slice

Flutter (`user_app`) integration is deferred, not silently skipped: wiring the new
endpoints into the existing GetX `AuthRepository`/`LoginBinding`/`LoginScreenController`
is a multi-file change (request/response models, error/loading states, secure-storage
session persistence) that deserves its own task and its own verification pass against
a running backend, rather than an unverified bolt-on here. See TASKS.md TASK-002.

## Verification performed

`mvn test` for the full backend reactor (`platform-web`, `identity-service`,
`building-service`, `api-gateway`): 51 tests, 0 failures/errors. New coverage: 9 unit
tests for the OTP lifecycle (in-memory fakes, no I/O), 4 integration tests against a
real Postgres testcontainer covering start→verify success with a round-trip JWT
validated against the service's own published JWKS, wrong-code rejection, and an
unseeded phone still authenticating without the `SUPER_ADMIN` claim. Existing
`PlatformMetadataSecurityTest`/`ReadinessDatabaseDownTest`/`GatewayRoutingTest` suites
(unrelated to this feature) still pass unmodified in behavior; `GatewayRoutingTest`
gained one new case proving the OTP start route is reachable without a bearer token.
`flutter analyze`/toolchain not exercised — no Flutter code changed.
