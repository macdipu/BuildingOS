# BOS-010 — Planning in progress (full restart)

Supersedes [BOS-001](../BOS-001) and [BOS-002](../BOS-002) after the operator asked to
restart from INTAKE under the revised BRD (multi-tenant SaaS: global identity,
platform roles, building application/approval lifecycle, subscription/entitlements,
back-office console). See [context-index.yaml](../../context-index.yaml) for the
supersession record.

- [Feature and scope](FEATURE.md)
- [Scoped baseline](BASELINE.md)
- [Requirements draft](REQUIREMENTS.md)
- [Decisions requiring resolution](DECISIONS.md)

Current gate: REQUIREMENTS, READY. D-01 (dev OTP `000000`) and D-02 (deploy-time
seed-script first `SUPER_ADMIN`, phone `01306999005`) carry forward from BOS-002,
unchanged. D-07/D-08 are now resolved:

- **D-07** — `PLATFORM_ADMIN` has full approve/reject/activate/suspend authority over
  building applications, identical to `SUPER_ADMIN`, no threshold.
- **D-08** — subscription plans are **back-office admin-configured data** (plan CRUD
  in the console), not a hardcoded MVP catalog. At least one plan must exist, created
  by a back-office operator, before any building application can be approved into an
  active subscription; the approval flow must return an explicit blocked/error state
  if none exists yet, never fabricate a default plan.

D-09 (back-office web tech) and D-10 (support/onboarding scope enumeration) remain
open as technical-design inputs, deferred to TECHNICAL, not REQUIREMENTS blockers.

- [Technical spec (TASK-001 slice)](TECH-SPEC.md)
- [Task sequence](TASKS.md) / [TASK-001 detail](tasks/TASK-001.md)

Current gate: TECHNICAL, READY. TASK-001 (global identity, platform roles, seed
SUPER_ADMIN, phone+OTP login, local token issuance) is **implemented and tested** —
backend only. 51/51 backend tests pass (`mvn test` from `backend/`). D-09/D-10 remain
open but are deferred to later, un-started features (back-office console; assisted
onboarding/support), not blockers for TASK-001.

Run: `RUN-D10A3F4277E7424F938E3BC2C46E9A2D`. Transition to IMPLEMENTATION hit a stale
context error (docs written after the last `context` registration); run is parked at
TECHNICAL/RUNNING, not corrupted — re-run `context` with the current file list, then
`transition ... IMPLEMENTATION` to continue the governed sequence, or treat TASK-001 as
done and move directly to TASK-002 (Flutter integration) / TASK-003 (production token
issuance) as ordinary engineering. Building application/lifecycle, units/ownership,
subscription/entitlements, and the back-office console remain separate, un-started
features under this EPIC.
