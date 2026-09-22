# BOS-010 decision register

Status: RESOLVED at REQUIREMENTS level. D-01/D-02 carry forward from BOS-002 as
pre-confirmed candidates. D-07/D-08 are now resolved by the operator. D-09/D-10 remain
technical-design inputs for TECHNICAL, not REQUIREMENTS blockers.

| ID | Decision needed | Known source constraint | Needed before |
|---|---|---|---|
| D-01 (carried) | Development OTP: `DevelopmentOtpProvider` accepts `000000` for active challenges, local/test only, behind a replaceable adapter. Phone-only login working interpretation; Google deferred. | BOS-002 operator decision, 2026-09-22; unaffected by the platform-role/SaaS revision | Login/session implementation |
| D-02 (carried) | First `SUPER_ADMIN` is provisioned by a deploy-time seed script/config (seed phone `01306999005`); authenticates via normal phone+OTP; no admin panel or password. Consistent with the revised model's platform-scoped roles (§110.1) since `SUPER_ADMIN` is explicitly platform-scoped, not building-scoped. | BOS-002 operator decision, 2026-09-22 | Platform-role bootstrap implementation |
| D-07 | RESOLVED: `PLATFORM_ADMIN` has full approve/reject/request-information/activate/suspend authority over building applications, the same as `SUPER_ADMIN` — no value/size threshold, no forced escalation. Both roles are checked identically by the approval use case. | Operator decision, 2026-09-22; §4.1, §149.4-149.7 | Building-application approval use case authorization check |
| D-08 | RESOLVED: subscription plans are **admin-configurable, not hardcoded**. No fixed MVP plan catalog is seeded/shipped. An authorized back-office role (`SUBSCRIPTION_ADMIN`/`PLATFORM_ADMIN`/`SUPER_ADMIN` per D-07-style checks) creates/edits `SubscriptionPlan` records (name, `unit_limit`, `user_limit`, `storage_limit`, enabled features, trial length) through the back-office plan CRUD screens/APIs already specified at §149.19 (`POST /api/v1/platform/subscription-plans`). Bootstrapping consequence: at least one plan must exist, created by a back-office operator, before any building application can be approved into `ONBOARDING` with an attached subscription (BA-03) — this is an operational prerequisite, not a hardcoded seed value. | Operator decision, 2026-09-22; §8.12, §110.3, §149.15-149.16,§149.19,§149.23 | Subscription plan CRUD implementation; back-office plan-management screen; approval flow's "attach initial subscription" step must handle "no plan configured yet" as an explicit blocked/error state, never a fabricated default |
| D-09 | Back-office web app technology. BRD suggests Flutter Web "to stay aligned with the primary client stack" but explicitly does not mandate it (§149.1). | §149.1 | Technical design / TECHNICAL stage, not a REQUIREMENTS blocker |
| D-10 | Exact assisted-onboarding/support-session `access_scope`/`permission_scope` enumeration and approval workflow for elevated support actions. BRD already specifies the entities, statuses and restrictions in detail (§149.11-149.12); only the precise scope-value enumeration and elevated-approval routing remain open. | §149.11-149.12 | Authorization design for onboarding/support agents |

D-09 and D-10 are technical-design inputs, not REQUIREMENTS blockers (same treatment as
D-04-D-06 in the superseded BOS-002 register).

No credentials are recorded in this document.
