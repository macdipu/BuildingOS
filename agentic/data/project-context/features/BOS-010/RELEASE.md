# BOS-010 — Release readiness (TASK-001/TASK-002 identity slice)

Run `RUN-D10A3F4277E7424F938E3BC2C46E9A2D`, scope revision 4. 2026-09-23.

Release scope: local/test phone+OTP login only (backend identity-service +
gateway public auth paths; Flutter login/OTP screens). **No production
deployment.** All new beans are `@Profile({"local","test"})`; outside those
profiles no OTP verifier/issuer bean exists and the service fails to start
rather than falling back to the fixed code.

## Checklist

| Item | Evidence | Status |
|---|---|---|
| Implementation | commits `c92e56c`, `39842eb`, `9453098` | Done |
| Automated tests | backend `mvn -B verify` 51/51; Flutter 22/22; `flutter analyze` clean | Pass |
| Code review | [REVIEW-QA.md](REVIEW-QA.md) REVIEW section — no blocking defects | READY |
| QA (live device) | [REVIEW-QA.md](REVIEW-QA.md) QA section, [qa/](qa/) screenshots; commit `7d29c86` | READY |
| Technical approval | operator, rev 4 | Approved |
| Release approval | operator, 2026-09-23 | Approved |
| UAT | not required by platform policy | N/A |
| DB migration | identity `V2__auth_identity.sql` (Flyway, forward-only, additive tables) | Applied in local/test |
| Rollback | revert commits; local DB reset via `docker compose ... down -v` (dev data only) | Feasible |
| Monitoring | actuator readiness on 8080/8081 verified during QA | Local only |

## Decisions recorded at release

- **Session restore on launch (operator, 2026-09-23): always start at login.**
  Current behavior (`AppPages.initial` = login) is the intended rule; the
  persisted session is not used to skip login on cold start.

## Residual risks / carried forward

- TASK-003: production token issuer and OTP/SMS provider undecided — blocks
  any non-local deployment.
- Low: `OtpChallenge.isExpired` inclusive at the exact expiry instant.
- Test gaps: concurrent double-verify, full JWT claim assertions, CORS preflight.
- Architecture follow-ups (audit 2026-09-23): legacy password login / reset_pin
  dead code, JSON in Flutter domain models, `ApiClient` navigation, seeder
  bypassing `UserRepository`, JWKS controller placement, no ArchUnit rules.
