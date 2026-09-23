# BOS-010 TASK-003 — Code review

Run `RUN-0CF5A7A84513476697D476E1334896C1`, stage REVIEW.

## Code review (agent, 2026-09-23)

Scope: uncommitted `backend/account-service` auth changes on HEAD `6a19a36`, plus
`docs/AUTH_CONFIGURATION.md`, `docs/ARCHITECTURE.md`, `docs/LOCAL_DEVELOPMENT.md`.
Verdict: **READY** (agent review, not human approval).

| Check | Result |
|---|---|
| `mvn -B -f backend/pom.xml verify` | exit 0; 80 tests, 0 failures/errors (Docker/Testcontainers) |
| `sh scripts/verify-flutter.sh` | VERIFY PASSED; 32 tests |
| `sh scripts/verify-platform.sh` | VERIFY PASSED (V3 migrates and re-runs idempotently; restart persistence) |
| `python3 scripts/check-contracts.py`; compose `config --quiet` | pass |
| Architecture | Ports `OtpProvider`/`SmsSender` in application; crypto/JDBC adapters in infrastructure; controllers transport-only; ArchUnit unchanged, passing |
| Scope 1 OTP boundary | SecureRandom 6 digits; HMAC-SHA256(pepper, challengeId‖code); constant-time compare; hash saved before send; sender failure consumes challenge, generic message, no cause |
| Scope 2 issuer | Mounted JWK Set, active kid, ≥2048 bits, unique kids, RS256/sig only, private/public pair probe; JWKS public-only incl. retained keys; TTL configurable; no fallback on bad explicit config |
| Scope 3 gates | Profile gates removed from `AuthConfiguration`, `AuthController`, `JwkSetController`; `SuperAdminSeeder` + seed use case stay local/test |
| Scope 4 abuse | Advisory-lock serialized per-phone limits (60 s, 5/rolling hour) → 429 `OTP_RATE_LIMITED`; expiry exclusive; consume rechecks expiry |
| Scope 5 migration | `V3` additive: nullable `code_hash`, `(phone, created_at)` index |
| Secrets in logs | Startup failure messages contain no pepper/key material; sender exceptions suppressed |

Findings:
1. Fixed (low, test quality): `AuthStartupTest` asserted only `hasFailed()` for
   `development` outside local/test; that context also fails on missing signing keys, so the
   guard was untested. Now asserts the root-cause message for both provider guards.
2. Follow-up, pre-existing (medium): phone strings are not canonicalized (only an optional
   leading `+` is folded for limits). `01711…` and `+8801711…` are distinct challenges and
   distinct users, so limits can be multiplied by format variants. Needs a business rule
   (canonical E.164 / BD numbering) — not invented here; resolve before a real SMS vendor.
3. Follow-up (medium, vendor task): no per-IP or global SMS send limit (SMS-pumping cost
   control). Out of TASK-003 scope; required input for the SMS vendor task.
4. Accepted: Flutter shows 429 as a generic error (recorded out-of-scope follow-up). In
   local/test, re-starting within 60 s of a previous start now returns 429.

## Automated QA (agent, 2026-09-23)

Revision: HEAD `6a19a36` + uncommitted TASK-003 working tree (incl. review test fix).
Verdict: **READY** for automated scope.

| Acceptance | Evidence | Result |
|---|---|---|
| 1 Dev login in local/test; suites pass | `mvn -B verify` re-run after review fix: 80 tests, 0 failures; `verify-flutter.sh` 32 passed; live smoke below | PASS |
| 2 `sms` provider hash/send/verify/binding | `OtpProviderIntegrationTest` (hash only, one send, wrong code `INVALID_CODE`, single use, cross-challenge fails, sender failure) | PASS (test sender; no vendor by decision) |
| 3 Fail closed | `AuthStartupTest`, `RsaJwtIssuerTest`; live jar without local profile: `sms` → exit 1 (no `SmsSender`), `development` → exit 1 ("Development OTP requires local/test profile") | PASS |
| 4 Active/retained kid, public-only JWKS | `RsaJwtIssuerTest` rotation; live JWKS fields `e,kid,kty,n` only | PASS |
| 5 429 + exclusive expiry | `OtpLoginIntegrationTest`, `OtpProviderIntegrationTest` boundaries/concurrency; live repeat start → 429 `OTP_RATE_LIMITED` (also with `+` stripped) | PASS |
| 6 `mvn verify` green, no secrets in logs | 80/0; smoke + fail-closed logs contain no token, pepper, or private JWK fields | PASS |

Live API smoke: account-service jar, `SPRING_PROFILES_ACTIVE=local`, compose Postgres,
port 18081 (8081 held by an older pre-existing process, left untouched).
start 200 → `111111` 401 `OTP_INVALID_CODE` → `000000` 200 token → reuse 401
`OTP_ALREADY_CONSUMED` → restart within 60 s 429 → unauthenticated `/internal/platform/info` 401.

Not covered: real SMS delivery (no vendor adapter by operator decision); on-device Flutter
run (no app code changed; 429 shown as generic error — recorded follow-up); human QA.
