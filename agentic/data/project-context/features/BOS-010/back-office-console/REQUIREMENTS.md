# BOS-010 F6 — Back-office console

Status: REQUIREMENTS_READY (revision 1, 2026-09-25).
Run: `RUN-8AAD6211653349D5BB3702342862F437` (new_feature, FEATURE, NO_REPLAN).
Source: BRD §149.1-149.2, §149.11-149.12, §149.17-149.19; parent
[REQUIREMENTS.md](../REQUIREMENTS.md) BO-01, BO-02, ON-03; [DECISIONS.md](../DECISIONS.md)
D-09, D-10.
Depends on: F2 (building-application API), F5a (subscription-plans/fees API) — both
already implemented and reused here, not rebuilt.

## Scope

| ID | Requirement and acceptance | Source |
|---|---|---|
| BOC-01 | New `buildingos_backoffice_web` project, separate tech stack from `user_app` (D-09: e.g. React/Next.js). Own navigation and platform-permission checks; no authoritative business logic in the UI — every operation calls existing or new API Gateway routes. Own session/auth using the same platform JWT issuer as `user_app` (no separate identity system). | §149.1, BO-01, D-09 |
| BOC-02 | Navigation shell per §149.2: Dashboard; Buildings (Applications/Under Review/Onboarding/Active Buildings/Suspended Buildings/Archived Buildings); Users (Platform Users/Building Admins/Onboarding Agents/Support Agents); Subscriptions (Plans/Building Subscriptions/Trials/Past Due-Grace Period/Suspended-Cancelled); Support (Assisted Onboarding/Active Support Sessions/Support History); System (Platform Settings/Feature-Entitlement Configuration/Audit Logs/System Health). | §149.2, BO-01 |
| BOC-03 | Buildings section reuses the existing building-application/building lifecycle API as-is (`/api/v1/platform/building-applications*`, `/api/v1/platform/buildings/{id}/{activate,suspend,reactivate}`); no new backend endpoints needed for this section. "Archived Buildings" has no backend state yet (`ARCHIVED` is explicitly out of scope per F2/AP-09) — the nav entry exists but shows an explicit "not available yet" state, never a fabricated list. | §149.2, §149.4, AP-09 |
| BOC-04 | Subscriptions section reuses the existing subscription-plans/fees API as-is (`/api/v1/platform/subscription-plans*`, `/api/v1/platform/free-tier`, `/api/v1/platform/fees/{feeCode}*`, `/api/v1/platform/users/{userId}/subscription`); no new backend endpoints needed for Plans/Trials/Past-Due views beyond what these already expose. | §149.15-149.16, RV-04 |
| BOC-05 | New platform-user management API (building-service or auth-service — TECHNICAL decision): list platform users with their platform role(s) and building memberships summary; view one user's detail (phone, platform roles, buildings/roles, subscription status via existing per-user subscription endpoint); assign/revoke a platform role (`SUPER_ADMIN`/`PLATFORM_ADMIN`/`ONBOARDING_AGENT`/`SUPPORT_AGENT`/`SUBSCRIPTION_ADMIN`), restricted to `SUPER_ADMIN` (a building admin cannot self-grant a platform role, ID-04). Every role assignment/revocation is audited. | §149.2, ID-04, AU-01 |
| BOC-06 | New `AssistedOnboardingSession` API: create (building, assigned agent, `access_scope` — one or more of the D-10 onboarding scope enum, reason, expiry), list/detail, complete/cancel. Session grants a temporary, building/scope-limited `ONBOARDING_AGENT` assignment; access is rejected once `expires_at` passes or status leaves `ASSIGNED`/`IN_PROGRESS`/`WAITING_FOR_CUSTOMER`. Every mutation made under an active session records both the acting agent and the building/customer context. The customer (via `user_app`, out of scope for this feature's implementation but the data must support it) can see that assisted onboarding is active for their building. | §149.11, ON-03, D-10 |
| BOC-07 | New `SupportSession`/`SupportAccessGrant` API: create (target user and/or building, `permission_scope` — one or more of the D-10 ordinary support scopes, reason, expiry), list/detail, end. No unrestricted silent impersonation: every session is visible in Support History with actor, target, scope, reason, start/end. Requesting a high-risk scope (`SUPPORT_REVERSE_PAYMENT`, `SUPPORT_TRANSFER_OWNERSHIP`, `SUPPORT_REMOVE_BUILDING_ADMIN`, `SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED`) is blocked by default and instead creates a pending elevated-approval request; only `SUPER_ADMIN` may approve it (D-10b), and approval is itself audited separately from the underlying support session. | §149.12, BO-02, D-10 |
| BOC-08 | New System section read APIs: Platform Settings (existing config surfaces only — no new settings invented here), Feature/Entitlement Configuration (reuses F5a's free-tier/plan entitlement API, read+edit already exists via BOC-04's reused endpoints), Audit Logs (a queryable view over the existing audited-transition/role-assignment/session records from AU-01, BOC-05, BOC-06, BOC-07 — not a new audit-writing mechanism, a read/list API over what already gets written), System Health (aggregates each service's existing `/actuator/health/readiness`, no new health-check logic invented). | §149.18, §149.21, AU-01 |
| BOC-09 | Dashboard section: read-only summary counts/widgets drawn from BOC-03/BOC-04/BOC-05/BOC-06/BOC-07's own APIs (e.g. applications pending review, active buildings, active support sessions, entitlement/plan counts) — no new business metric invented beyond what those APIs already return. | §149.18 |

## Out of scope

Payment-gateway integration and live SMS (tracked elsewhere, e.g. BOS-004); mobile app
(`user_app`) changes beyond what BOC-06 already requires it to support reading (no
implementation of that customer-visible indicator is claimed by this feature — flagged
as a dependency, not built here); `Archived Buildings` backend state (BOC-03 explicitly
shows "not available yet"); any new business rule not already decided in
[DECISIONS.md](../DECISIONS.md) — if TECHNICAL design surfaces one, it goes back to the
operator as a new decision, not invented here.
