# BOS-010 TASK-007 — Code review and automated QA

Run `RUN-F334D51A33AE4944B2F04BE708F7BBE1`. 2026-09-23. Revision: HEAD `6033884` + uncommitted
account-service change. Verdicts: review **READY**, QA **READY** (agent, not human approval).

## Code review

| Check | Result |
|---|---|
| Layering | `PhoneNumber` is a framework-free domain value object; services parse at the use-case boundary; JDBC adapter receives canonical values only; ArchUnit passes unchanged |
| Rule | Accepts `0`, `880`, or `+880` followed by `1[3-9]` + 8 digits; canonical `01[3-9]XXXXXXXX`; constructor enforces canonical form |
| Start | Invalid → `IllegalArgumentException` → existing 400 `INVALID_REQUEST` |
| Verify | Unparseable → `PHONE_MISMATCH` (no new code); compare + `findOrCreateByPhone` use canonical |
| Rate limit | `+`-folding hack removed; lock/count by canonical phone |
| Seed | `SeedSuperAdminService` parses (seed `01306999005` already canonical) |

Findings:
1. Fixed during implementation: first regex expected `880` + `01…`; the international form
   drops the trunk `0` (`880` + `1…`). Caught by `PhoneNumberTest`; now `(?:0|\+?880)(1[3-9]\d{8})`.
2. Accepted (per design, no migration): rows stored earlier in non-canonical form (local/test
   only, e.g. users created by direct API calls with `+880…`) are no longer reachable; the next
   login creates a canonical user. App-created and seeded data were already canonical.

## Automated QA

| Acceptance | Evidence | Result |
|---|---|---|
| 1 Same limits + user across forms; canonical stored/returned | `OtpChallengeFlowTest.anyAcceptedPhoneFormVerifiesTheSameCanonicalUser`; `OtpProviderIntegrationTest.concurrentStartsInAnyPhoneFormCannotBypassLimits` (9 parallel starts across 3 forms → 1 accepted); `OtpLoginIntegrationTest.rateLimitIsSharedAcrossPhoneForms` (HTTP 429) | PASS |
| 2 Invalid → 400 start; unparseable verify → mismatch | `PhoneNumberTest` (22 cases); `nonBangladeshMobileNumberIsRejectedAtStart` (HTTP 400); `unparseablePhoneAtVerifyIsAMismatch` | PASS |
| 3 Suites | `mvn -B -f backend/pom.xml verify`: 106 tests, 0 failures; `verify-flutter.sh` VERIFY PASSED (35) | PASS |

Not covered: live device run (no app change); human QA.
