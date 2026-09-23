# TASK-003 — Post-cleanup readiness

Date: 2026-09-23. Source revision: `6a19a36`.
Run: `RUN-0CF5A7A84513476697D476E1334896C1`. Classification: existing task, brownfield,
TASK_ONLY, NO_REPLAN. Baseline is usable; technical reapproval is pending.

## Confirmed decisions and scope

The task's recorded operator decisions supersede the stale handoff: account-service
will issue tokens itself; the real SMS vendor is deferred. Implement the provider
boundary and a test-only sender. TASK-004 is completed, with architecture and
verification evidence in [its task](tasks/TASK-004.md).
The prior TASK-003 run was cancelled before implementation, and explicitly requires
technical reapproval after the cleanup. This refresh does not grant that approval.

## Source findings and implementation mapping

Paths below are relative to
`backend/account-service/src/main/java/com/buildingos/account/auth/`.

| Existing source | Confirmed state / proposed change |
|---|---|
| `application/startotp/StartOtpService.java` | Currently inserts a challenge only. Add atomic start limits, then invoke provider issuance after persistence. |
| `application/verifyotp/VerifyOtpService.java` | Currently calls the phone/code-only verifier. Pass the stored challenge to the new provider; retain attempt counting and single-use consumption. |
| `application/port/out/` | Replace `OtpCodeVerifier` with `OtpProvider`; add `SmsSender`. Keep `TokenIssuer` and `SigningKeyProvider` contracts. |
| `domain/model/OtpChallenge.java` | No hash; expiry currently accepts the exact expiry instant. Add nullable hash and reject at or after expiry. |
| `domain/repository/OtpChallengeRepository.java` | Add hash persistence and an atomic rate-limited start contract. Keep interfaces free of framework dependencies. |
| `infrastructure/persistence/repository/JdbcOtpChallengeRepositoryAdapter.java` | No rate limiting; consume currently checks only single use. Enforce start limits transactionally across instances; recheck expiry during atomic consumption. |
| `infrastructure/persistence/mapper/OtpChallengeRowMapper.java` | Extend mapping for nullable hash. |
| `infrastructure/security/LocalRsaJwtIssuer.java` | Fresh in-memory RSA key per process, fixed 15-minute TTL. Replace with configured JWK Set loading, active kid selection, public-only publication, configurable positive TTL. |
| `infrastructure/config/AuthConfiguration.java` | Currently entirely local/test. Move fail-closed selection into configuration; preserve local/test-only seeding. |
| `presentation/rest/{AuthController,JwkSetController}.java` | Both still profile-gated. Remove gates with provider/key validation; JWKS controller already calls its use case in the correct layer. |

Use the existing per-action application packages and JDBC repository adapters.
Crypto and SMS adapters belong in infrastructure; controllers remain transport-only.
Migration `V3__otp_code_hash.sql` adds the hash and phone/time index; V1/V2 remain
unchanged. DB/migration implementation follows the DB/Integration persona.

## Design details to carry into implementation

- Retain TASK-003 defaults: 60-second cooldown, five starts per rolling hour,
  five-minute challenge lifetime, five verification attempts, 15-minute token TTL.
- Serialize rate check plus insert for the same phone in the database so parallel
  requests cannot bypass either limit. Test independent connections.
- Store the challenge-bound HMAC before calling the sender. Never return success
  after a send failure or automatically resend on an ambiguous external outcome.
  Do not claim exactly-once delivery across crashes; the test sender records one
  invocation per successful start.
- Missing sender/pepper, unsafe development selection, invalid signing configuration,
  or an unusable active private key must fail startup. Do not log codes, pepper,
  private JWK contents, or tokens. Keep existing HTTPS issuer/JWKS validation.
- Preserve `ApiEnvelope` / `ApiError`; map throttling to HTTP 429
  `OTP_RATE_LIMITED` through application results and REST mapping.
- Rotation publishes old and active public keys. Loading the mounted set is startup
  configuration; automated rotation and live reload remain outside scope.

## Verification plan

Extend `OtpChallengeFlowTest` and `OtpLoginIntegrationTest`; add provider,
configured-key, and startup-validation tests. Cover generated code/hash binding,
wrong codes, reuse, expiry equality, sender failure, cooldown/hour boundaries,
concurrent starts, active/retained-key verification, public-only JWKS, and all
fail-closed configurations. Existing ArchUnit rules remain unchanged.

Run `mvn -B -f backend/pom.xml verify` with Docker available, then the existing
Flutter regression check (`sh scripts/verify-flutter.sh`). Adapt test clocks/fixtures
where new limits intentionally affect repeat starts; do not disable production limits.

This continuation checked source and ran harness `doctor` successfully.
No application tests were rerun and no implementation code was changed.

## Next action

Obtain the technical reapproval explicitly required by
[TASK-003](tasks/TASK-003.md), record it against this run's CONTEXT evidence, then
transition the same run to IMPLEMENTATION. No SMS vendor decision is needed for
this bounded task. Actual production SMS login remains unavailable until a real
sender adapter is supplied in later work.
