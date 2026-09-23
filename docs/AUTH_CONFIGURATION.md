# Account authentication configuration

BOS-010 TASK-003 adds persistent RSA signing keys and a provider-neutral SMS boundary.
The account service issues access tokens. A production SMS adapter is intentionally
not included: selecting SMS without an `SmsSender` bean fails startup.

## Configuration

| Setting | Default / contract |
|---|---|
| `BUILDINGOS_OTP_PROVIDER` | `development` in local/test, `sms` otherwise; unknown values fail startup |
| `OTP_CODE_PEPPER` | Required for SMS; secret containing at least 32 UTF-8 bytes |
| `OTP_RESEND_COOLDOWN` | ISO-8601 duration, default `PT60S`, must be positive |
| `OTP_MAX_STARTS_PER_HOUR` | Positive integer, default 5 |
| `ACCOUNT_SIGNING_JWKS_PATH` | Mounted JWK Set JSON; required outside local/test |
| `ACCOUNT_SIGNING_ACTIVE_KID` | Required with a key file; identifies an RSA private signing key |
| `ACCOUNT_ACCESS_TOKEN_TTL` | ISO-8601 duration, default `PT15M`, at least one second |
| `JWT_ISSUER`, `JWT_JWK_SET_URI`, `JWT_AUDIENCE` | Existing validation settings; HTTPS URLs required outside local/test |

The equivalent Spring properties are `buildingos.otp.provider`,
`buildingos.otp.code-pepper`, `buildingos.otp.cooldown`,
`buildingos.otp.max-per-hour`, and `buildingos.account.{signing-jwks-path,signing-active-kid,access-token-ttl}`.

Local/test may generate an in-memory RSA key only when no key path or active kid is
configured. A bad explicit configuration never falls back to a generated key.
Development OTP is rejected outside local/test. SUPER_ADMIN seeding remains local/test.

## Mounted signing keys and rotation

Use RSA keys of at least 2048 bits with unique, nonempty kids. The active key needs
private material; retained keys may be public-only. Published JWKS contains public
fields only. Keys are loaded at startup; use a controlled restart to apply changes.

For rotation, distribute the next public key to every instance's retained set before
switching active signing keys; allow downstream JWKS caches to refresh. Retain the
previous public key until all tokens signed by it have expired, allowing for the
validators' configured clock skew. Keep the issuer URL and audience stable.

Mount the private set as a restricted secret. Do not commit the key file, pepper,
or real SMS credentials. Automated key management, refresh tokens, session revocation,
and deployment are outside TASK-003.

## OTP behavior and errors

Start and verify retain their existing request and success envelopes.
`POST /api/v1/auth/otp/start` returns HTTP 429 with `ApiError.code=OTP_RATE_LIMITED`
during cooldown or after five starts in the rolling hour. Limits apply to development
and SMS providers. An optional leading plus cannot bypass the per-phone limit; this
does not introduce general phone-number normalization.

SMS mode generates six digits, stores only a challenge-bound HMAC-SHA256, then invokes
the sender once. The plaintext code exists only transiently for delivery. A failed
sender invalidates the challenge and returns a generic server error; its vendor
exception is not logged or returned. The start remains counted against limits.
There is no automatic resend after an ambiguous delivery result.

Challenges expire at the five-minute boundary, allow five verification attempts, and
can be consumed only once. Start admission and consumption use atomic database
operations across instances. Migration V3 adds a nullable hash and phone/time index;
development challenges leave the hash null. V1/V2 remain unchanged.

A real sender adapter and its delivery policy require a later task before production
SMS login can be used. Test senders exist only in test sources.
