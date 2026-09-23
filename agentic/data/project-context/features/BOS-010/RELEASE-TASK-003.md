# BOS-010 — Release readiness (TASK-003 signing keys + provider-neutral OTP)

Run `RUN-0CF5A7A84513476697D476E1334896C1`. 2026-09-23. Revision: HEAD `6a19a36` +
uncommitted TASK-003 working tree (not committed; operator commit request pending).

Release scope: account-service auth usable in local/test with development OTP and a
generated key; outside local/test the service **fails closed** until a mounted signing
JWK Set + active kid, `OTP_CODE_PEPPER`, and a real `SmsSender` adapter exist.
**No production deployment**; production SMS login unavailable until the vendor task.

## Checklist

| Item | Evidence | Status |
|---|---|---|
| Implementation | uncommitted working tree; [task-003-implementation-result.json](task-003-implementation-result.json) | Done |
| Automated tests | `mvn -B verify` 80/0; Flutter 32 passed; `verify-platform.sh`, `check-contracts.py` pass | Pass |
| Code review | [REVIEW-TASK-003.md](REVIEW-TASK-003.md) — no blocking defects | READY |
| QA | [REVIEW-TASK-003.md](REVIEW-TASK-003.md) QA section — acceptance 1-6 pass, live API smoke, live fail-closed | READY |
| Technical approval | operator (macdipu), 2026-09-23 | Approved |
| Release approval | operator (macdipu), 2026-09-23 | Approved |
| UAT | not required by platform policy | N/A |
| DB migration | account `V3__otp_code_hash.sql` (additive nullable column + index, forward-only) | Applied in local/test |
| Rollback | revert working-tree change; V3 is additive — old code ignores `code_hash`; drop column only with operator OK | Feasible |
| Config | [docs/AUTH_CONFIGURATION.md](../../../../../docs/AUTH_CONFIGURATION.md) | Documented |

## Residual risks / carried forward

- Real `SmsSender` vendor adapter, sender ID/BTRC, cost policy — separate task.
- Per-IP / global SMS send limits (SMS-pumping) — required input for vendor task.
- Phone canonicalization (pre-existing): format variants get separate limits and users;
  needs business rule.
- Flutter shows 429 `OTP_RATE_LIMITED` as a generic error — follow-up.
- Refresh tokens, revocation, automated key rotation/KMS — out of scope.
- Resolves TASK-001/002 residual: `OtpChallenge.isExpired` now exclusive at expiry.
