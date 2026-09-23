# TASK-003 — Production token issuance + provider-neutral OTP/SMS adapter (backend)

## Status: DESIGN APPROVED, ON HOLD — architecture cleanup (TASK-004) runs first

First run `RUN-E94BAA68F8F4403FBDAC06C6B1A8E1AB` was approved (2026-09-23) then cancelled
before any code change at operator request. Restart as a new run after TASK-004;
re-approve the technical gate (package layout may shift after cleanup).
Must follow Clean Architecture + feature-first + use case + repository pattern, SOLID,
microservice ownership (account-service owns `account_db`).

## Operator decisions (2026-09-23)

- **Issuer:** `account-service` itself remains the token issuer in production
  (no external IdP). Production-grade key handling replaces the in-memory key.
- **OTP provider:** adapter only; SMS vendor chosen later. Build the provider-neutral
  boundary and a test-only fake sender; no vendor account, SDK, or live SMS.
- **Cold launch:** app always starts at login (recorded in RELEASE.md; no app change here).

## Current state (baseline, commit `e328481`)

- `AuthConfiguration`, `LocalRsaJwtIssuer`, `JwkSetController`,
  `DevelopmentOtpCodeVerifier`, `SuperAdminSeeder` are all `@Profile({"local","test"})`.
  Outside local/test the auth feature does not exist.
- `LocalRsaJwtIssuer` generates a fresh RSA key per process start (tokens die on restart,
  no multi-instance, no rotation).
- `OtpCodeVerifier.isValidCode(phone, code)` has no challenge context; there is no code
  generation or storage — only the dev fixed code `000000`.
- `PlatformWebConfiguration.jwtDecoder` already rejects non-HTTPS issuer/JWKS outside
  local/test.

## Scope

1. **OTP provider boundary (application layer)**
   - Replace `OtpCodeVerifier` with port `OtpProvider`:
     `issue(OtpChallenge)` (called on start, after the challenge row exists) and
     `verify(OtpChallenge, String code)`.
   - `DevelopmentOtpProvider` (renamed from `DevelopmentOtpCodeVerifier`): issues nothing,
     accepts `000000`; bean only in local/test.
   - `HashedCodeOtpProvider` (new, profile-neutral): generates a 6-digit code with
     `SecureRandom`, stores `HMAC-SHA256(pepper, challengeId ‖ code)` on the challenge,
     sends the code through port `SmsSender.send(phone, message)`, verifies with a
     constant-time compare. Pepper from secret config (`OTP_CODE_PEPPER`, ≥32 bytes).
   - `SmsSender` port with **no production adapter** in this task. A
     `RecordingSmsSender` exists only in test sources.
   - Selection by `buildingos.otp.provider` = `development` | `sms`.
     **Fail closed:** `development` outside local/test → startup failure; `sms` without an
     `SmsSender` bean or pepper → startup failure. Never fall back to `000000`
     (per BOS-002 OTP-PROVIDER.md).
2. **Production token issuer (infrastructure)**
   - `RsaJwtIssuer` replaces `LocalRsaJwtIssuer`: signing keys loaded from a JWK Set
     (private RSA keys) at `ACCOUNT_SIGNING_JWKS_PATH` (mounted secret); active key by
     `ACCOUNT_SIGNING_ACTIVE_KID`. All keys' public halves are published at
     `/.well-known/jwks.json` so a previous key keeps validating during rotation.
   - Local/test with no path configured: generated in-memory key (current behavior).
     Outside local/test with no path/kid, or kid not found, or key < 2048 bits →
     startup failure.
   - Access-token TTL configurable (`ACCOUNT_ACCESS_TOKEN_TTL`, default 15 min, unchanged).
   - `JwkSetController` moves to `presentation/rest` (architecture audit).
3. **Enable auth outside local/test:** drop the profile gate from `AuthConfiguration`;
   gating now lives in provider/key selection above. `SuperAdminSeeder` stays
   local/test only (deploy-time seed per D-02 is a separate ops script).
4. **Abuse controls (technical-design inputs; values configurable, defaults proposed):**
   - Resend cooldown per phone: **60 s** (matches the app's resend timer).
   - Max OTP starts per phone: **5 per rolling hour** → HTTP 429 `OTP_RATE_LIMITED`.
   - Existing: 5-minute challenge TTL, 5 verify attempts, single use.
   - `OtpChallenge.isExpired` becomes exclusive at the expiry instant (review low finding).
5. **Migration `V3__otp_code_hash.sql`:** `ALTER TABLE otp_challenge ADD COLUMN
   code_hash varchar(64)` (nullable; dev provider leaves it null) and index
   `(phone, created_at)` for rate limiting. Additive, forward-only.

## Out of scope (not invented here)

- Real SMS vendor adapter, sender ID/BTRC registration, cost policy.
- Refresh tokens, session revocation, device binding (D-03 unresolved).
- Key-management service integration beyond a mounted JWK Set file; automated rotation.
- Flutter changes, except that the app must handle the new 429 `OTP_RATE_LIMITED`
  code — handled as a generic error today; a dedicated message is a follow-up.

## Acceptance

1. Local/test behavior unchanged: `000000` works, existing 51 backend + 22 Flutter tests pass.
2. `sms` provider: start stores a hash (never the code), sends exactly one SMS with the
   code; right code verifies once; wrong code → `OTP_INVALID_CODE`; code bound to its
   challenge (another challenge's code fails).
3. Startup fails closed for: `development` outside local/test; `sms` without sender or
   pepper; non-local without signing key file/kid; kid missing; short key.
4. Token signed with the configured active kid validates via the published JWKS; a token
   signed with a retired (still published) key validates; JWKS never contains private parts.
5. Cooldown and hourly limit return 429 `OTP_RATE_LIMITED`; expiry exclusive at instant.
6. `mvn -B verify` green (Docker); no secrets in logs.
