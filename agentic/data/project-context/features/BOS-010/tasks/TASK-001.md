# TASK-001 — Global identity, platform roles, seed SUPER_ADMIN, phone+OTP login

## Status: DONE (backend), verified by tests. Flutter integration is TASK-002.

## Acceptance criteria

1. A global `User` (phone-based identity) exists, never duplicated per building —
   `app_user` table, one row per phone. ✅ (`JdbcUserRepository`, `V2__auth_identity.sql`)
2. Platform roles (`SUPER_ADMIN, PLATFORM_ADMIN, ONBOARDING_AGENT, SUPPORT_AGENT,
   SUBSCRIPTION_ADMIN`) are modeled independently of building membership. ✅
   (`PlatformRole` enum, `platform_user_role` table)
3. The first `SUPER_ADMIN` is provisioned by a deploy-time seed mechanism (seed phone
   `01306999005`), not through the login/sign-up flow; no admin panel, no password.
   Idempotent; tolerates database-unavailable-at-startup without crashing the process.
   ✅ (`SuperAdminSeeder`, local/test profile only)
4. Phone+OTP login: start a challenge, verify with the fixed development code
   `000000` (local/test only), reject wrong/expired/consumed/unknown/exhausted
   attempts, treat the code as a string (leading zeros preserved). ✅
   (`StartOtpChallengeService`, `VerifyOtpChallengeService`, `DevelopmentOtpCodeVerifier`)
5. On successful verification, issue a session token carrying the user's platform
   roles; a client (or another service) can validate it without trusting the client's
   own claim of who it is. ✅ (`LocalRsaJwtIssuer` + `JwkSetController`, proven by
   `OtpLoginIntegrationTest` decoding the issued token against the service's own
   published JWKS)
6. The OTP endpoints are reachable through the gateway without a bearer token (they
   ARE the login step), while every other route is unaffected (still requires one).
   ✅ (`SecuritySettings.publicPaths`, `GatewayRoutes` new routes,
   `GatewayRoutingTest.otpStartIsPubliclyReachableWithoutAToken`)
7. None of this activates outside `local`/`test` profiles; a non-local deployment
   simply doesn't have this feature yet (fails to wire it, rather than silently
   allowing a fixed OTP code or an unsigned/self-issued token in production). ✅
   (`@Profile({"local","test"})` on every new bean; no unconditional bean depends on them)

## Test evidence

- `OtpChallengeFlowTest` (9 cases, in-memory fakes): correct code verifies; wrong code
  rejected; unknown attempt rejected; phone mismatch rejected; expired rejected;
  consumed-once cannot be reused; attempts exhausted after repeated wrong codes;
  malformed phone rejected at start; code treated as string.
- `OtpLoginIntegrationTest` (4 cases, real Postgres via Testcontainers, full Spring
  context): seeded `SUPER_ADMIN` phone logs in and its token carries the
  `SUPER_ADMIN` platform-role claim; an unseeded phone authenticates but gets no role
  claims; wrong code returns 401 `OTP_INVALID_CODE`; the JWKS endpoint is publicly
  reachable.
- `GatewayRoutingTest`: existing 9 cases unaffected; 1 new case proves the OTP route
  is public through the gateway.
- `PlatformWebConfigurationTest`, `PlatformMetadataSecurityTest` (both services),
  `ReadinessDatabaseDownTest` (both services): unaffected, still pass — proving the
  shared `platform-web` changes (public-paths, CORS, error handler) didn't regress
  existing security/readiness behavior.
- Full backend reactor `mvn test`: 51 tests, 0 failures/errors.

## Bugs found and fixed during this task (not pre-existing; introduced and fixed within it)

- `AuthController` was `final` and class-annotated `@Validated`, which requires a
  CGLIB subclass — Spring couldn't proxy a final class, breaking context startup.
  Fixed by removing the class-level `@Validated` (per-parameter `@Valid` on the
  `@RequestBody` is sufficient) and the `final` modifier.
- Binding `java.time.Instant` directly as a JDBC parameter isn't supported by the
  PostgreSQL driver (`Can't infer the SQL type to use for an instance of
  java.time.Instant`). Fixed by converting to `java.sql.Timestamp` before binding in
  `JdbcOtpChallengeRepository`.
- `SuperAdminSeeder` originally let a `DataAccessException` abort application startup
  when the database was unreachable, which would have broken the existing
  `ReadinessDatabaseDownTest` invariant ("database outage fails readiness, not
  liveness"). Fixed by catching and logging instead of propagating.
- Discovered (and fixed generally, in `PlatformErrorHandler`) that any unhandled
  exception anywhere in any service was falling through to the servlet container's
  default `/error` dispatch, which the security filter chain then rejected as
  unauthenticated — silently converting real 500s into misleading 401s. Added a
  catch-all `Exception` → 500 handler.

## Explicitly not covered

Flutter integration (TASK-002), production token issuance / real OTP provider
(TASK-003), building application/lifecycle, units/ownership, subscription, back-office.
