# BOS-010 implementation sequence (identity & platform roles feature only)

BOS-010 remains a provisional EPIC (see FEATURE.md). This sequence covers the
identity, platform roles, and phone OTP slice. Execute existing tasks without
recreating planning artifacts; TASK-003 uses TASK_ONLY / NO_REPLAN.

| Task | Outcome | Status |
|---|---|---|
| [TASK-001](tasks/TASK-001.md) | Backend global identity, roles, seed SUPER_ADMIN, development login | COMPLETED; local/test release in [RELEASE.md](RELEASE.md) |
| [TASK-002](tasks/TASK-002.md) | Flutter phone OTP integration | COMPLETED; automated and live QA in [REVIEW-QA.md](REVIEW-QA.md) |
| [TASK-004](tasks/TASK-004.md) | Architecture conformance and account-service rename | COMPLETED; [review/QA](REVIEW-TASK-004.md) |
| [TASK-005](tasks/TASK-005.md) | Locale toggle and login/OTP polish | COMPLETED; [review/QA](REVIEW-TASK-005.md) |
| [TASK-003](tasks/TASK-003.md) | Account-service signing keys and provider-neutral OTP/SMS boundary; vendor deferred | IMPLEMENTED (technical gate approved 2026-09-23); backend 80 / Flutter 32 tests green; [review](REVIEW-TASK-003.md) READY; COMPLETED; [release](RELEASE-TASK-003.md) local/test, production fail-closed |

Continuation run: `RUN-0CF5A7A84513476697D476E1334896C1`.
The old TASK-003 run was cancelled before code changes. TASK-004 is now complete.
[Post-cleanup readiness](TASK-003-READINESS.md) records the concrete design and
validation plan. Implementation evidence:
[task-003-implementation-result.json](task-003-implementation-result.json).

Do not commit without an explicit operator request.

Later features under this EPIC (not yet intake'd, see FEATURE.md/DECISIONS.md):
building application & lifecycle; units & ownership; subscription & entitlements;
back-office console (needs D-09/D-10).
