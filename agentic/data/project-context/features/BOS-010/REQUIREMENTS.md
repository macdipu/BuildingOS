# BOS-010 requirements draft

Status: DRAFT — not ready for technical approval or implementation.
Source: [BRD](../../BuildingOS_BRD_Agentic_Development.md), revised 2026-09-22. Open
choices are in [DECISIONS.md](DECISIONS.md). This is an EPIC; identifiers below group
into candidate features for PLANNING, not a single implementation task.

## User/operator journeys

**Customer (self-service):** logged out → phone OTP login → submit building
application → wait for review → (if approved) building enters onboarding → complete
guided setup → building active. **Existing member:** logged in → My Buildings/
Portfolio (multiple buildings, roles, owned units) → select active building →
building-scoped screens (unchanged BOS-002 shape: units, ownership, membership).
**Back-office operator:** phone+OTP login to the back-office console → review
building applications → approve/reject/request info → assign Building Admin →
manage subscription/plan → assign onboarding/support agents → audit trail.

## Requirement groups and observable acceptance

### G1 — Global identity & platform roles

| ID | Requirement and acceptance | BRD source | Pending |
|---|---|---|---|
| ID-01 | Phone login with country code and six-digit OTP through a replaceable provider; verify/resend/change-number; reject invalid/expired/reused; +880 support. | §§40-41,93,109 | D-01 (carried, resolved) |
| ID-02 | Google sign-in deferred; adapter boundary preserved for future providers. | User direction, carried from BOS-002 | none |
| ID-03 | One global `User` per person; never duplicated per building. Ownership/membership/tenancy are separate relationships from the same user. | §110.1, §149.13 | none |
| ID-04 | Platform roles (`SUPER_ADMIN`, `PLATFORM_ADMIN`, `ONBOARDING_AGENT`, `SUPPORT_AGENT`, `SUBSCRIPTION_ADMIN`) are managed independently of building membership/roles; a building admin cannot self-grant a platform role. | §4.1, §5, §149.22 | none |
| ID-05 | First `SUPER_ADMIN` provisioned by deploy-time seed script/config (seed phone `01306999005`), authenticates via normal phone+OTP; no admin panel/password anywhere in the design. | Carried BOS-002 D-02 | D-02 (carried, resolved) |

### G2 — Building application & lifecycle

| ID | Requirement and acceptance | BRD source | Pending |
|---|---|---|---|
| BA-01 | A building enters BuildingOS only through a `BuildingApplication` (sources: `SELF_SERVICE, ASSISTED, BACK_OFFICE, SALES, IMPORT`) unless an explicitly authorized migration/import workflow bypasses review. | §149.3 | none |
| BA-02 | Application state machine: `DRAFT → SUBMITTED → UNDER_REVIEW → (MORE_INFORMATION_REQUIRED → SUBMITTED | REJECTED | APPROVED) → ONBOARDING → ACTIVE`, then `ACTIVE ⇄ SUSPENDED → ARCHIVED`. A submitted application is never an active tenant. Every transition is audited. | §110.2, §149.4 | none |
| BA-03 | Approval creates/activates the canonical `Building`, assigns the initial `BUILDING_ADMIN`, creates `BuildingOnboarding`, attaches an initial subscription/trial using an existing back-office-configured `SubscriptionPlan`, and emits lifecycle events — without requiring all units/owners/finance settings to pre-exist. If no plan is configured yet, approval must return an explicit blocked/error state, never fabricate a default plan. | §149.7 | D-08 resolved |
| BA-04 | Back-office review screen shows application detail, duplicate-building signals (name/address/area/coordinates/contact — review signals only, never auto-merged), and actions `Start Review / Approve / Reject / Request More Information / Assign Onboarding Agent / Add Internal Note`; sensitive actions require confirmation + audit reason. Available to both `SUPER_ADMIN` and `PLATFORM_ADMIN`, identically. | §149.5, §149.6 | D-07 resolved |
| BA-05 | Approval/rejection/activation/suspension authorization is enforced by platform-role checks (`SUPER_ADMIN` or `PLATFORM_ADMIN`, no threshold between them), never inferred from an active-building header. | §149.22 | D-07 resolved |

### G3 — Guided/assisted building onboarding

| ID | Requirement and acceptance | BRD source | Pending |
|---|---|---|---|
| ON-01 | Onboarding modes `SELF_SERVICE / ASSISTED / BACK_OFFICE_SETUP` all produce the same canonical Building/Unit/Ownership records via the same validation. Resumable 11-step flow (building info → structure/floors → units → owners → committee/staff → maintenance config → rent-management config → payment methods → invitations → review → activate). | §149.8-149.9 | none |
| ON-02 | Activation minimum prerequisites: approved building, ≥1 active Building Admin, required identity/address data, ≥1 valid unit (unless migration policy permits otherwise), accepted platform terms where applicable. Optional setup may continue after activation. | §149.9 step 11 | none |
| ON-03 | Assisted onboarding: `AssistedOnboardingSession` grants a temporary, building/scope-limited `ONBOARDING_AGENT` assignment that expires automatically; no implicit unrelated-building access; every assisted mutation is audited and visible to the customer. | §149.11 | D-10 (exact scope enumeration) |

### G4 — Units & ownership (carried from BOS-002, unchanged domain rules)

| ID | Requirement and acceptance | BRD source | Pending |
|---|---|---|---|
| UN-01 | Unit creation/editing validates unit-number uniqueness within its building, positive area, allowed type (`FLAT/PARKING/STORAGE/COMMERCIAL/COMMON/OTHER`); concurrent duplicate creation cannot bypass uniqueness. Bulk generation/import supported (by floor/count/pattern, CSV/Excel). | §§8.2,49,110; §149.9 step 3 | none |
| OW-01 | Assign ownership with owner/share/effective date; history retained; aggregate active shares ≤100% enforced transactionally; one user may own multiple units in one building and across many buildings; co-owners supported. | §§19.1,51; §149.9 step 4, §149.13 | D-04-equiv (full matrix, technical stage) |
| OW-02 | Transfer requires confirmation, closes/opens ownership periods, preserves history, never mutates a tenant lease. | §§52,127,143E | D-05-equiv (temporal policy, technical stage) |
| BL-01 | `My Buildings`/portfolio screen lists all buildings the user belongs to (name, address, lifecycle status, roles, owned-unit count, alerts) plus `My Properties` (cross-building owned-unit aggregate), `Pending Invitations`, and `Building Applications` submitted by the user. | §41-42, §149.14 | none |

### G5 — Subscription & entitlements

| ID | Requirement and acceptance | BRD source | Pending |
|---|---|---|---|
| SUB-01 | Subscription is building-level by default; one user's membership across buildings never merges their subscriptions. States: `TRIAL, ACTIVE, PAST_DUE, GRACE_PERIOD, SUSPENDED, CANCELLED, EXPIRED`. | §8.12, §110.3, §149.15 | none |
| SUB-02 | Entitlements (`rent_management.enabled, maintenance.enabled, work_orders.enabled, reports.*, offline_sync.enabled, max_units, max_users, storage_limit_mb, support_tier`) are enforced authoritatively by backend; UI may only hide/disable for UX. Suspension never deletes customer data; restricted behavior during grace/suspension is explicit and policy-driven. | §149.16 | none |
| SUB-03 | Back-office **plan CRUD**: an authorized back-office role creates/edits/retires `SubscriptionPlan` records (name, unit/user/storage limits, enabled features, trial length) — no hardcoded MVP plan catalog is shipped; plans are entirely admin-configured data. Building-subscription actions (assign/change plan, start/extend/end trial, suspend/reactivate, cancel, inspect history/effective entitlements) reference these operator-defined plans. All actions audited. MVP excludes automated charging, payment-provider billing collection, invoices/tax automation, and organization-level consolidated billing. | §149.15,§149.19,§149.23 | none (D-08 resolved) |

### G6 — Back-office console, support & audit

| ID | Requirement and acceptance | BRD source | Pending |
|---|---|---|---|
| BO-01 | A distinct back-office application surface (own navigation, own platform-permission checks, no authoritative business logic in the UI) covering Dashboard / Buildings / Users / Subscriptions / Support / System navigation. | §149.1-149.2, §149.17-149.18 | D-09 (tech choice, technical stage) |
| BO-02 | Support assistance never implements unrestricted silent impersonation; a `SupportSession`/`SupportAccessGrant` records platform user, target user/building, reason, scope, approver, start/expiry/end; high-risk actions (payment reversal, ownership transfer, Building-Admin removal, unrestricted financial export) are blocked or require elevated, separately audited approval during ordinary support sessions. | §149.12 | D-10 |
| AU-01 | Audit at minimum: application state transitions, approval/rejection actor+reason, building activation/suspension/reactivation, Building-Admin assignment/removal, onboarding-agent assignment and assisted mutations, support-session lifecycle+actions, subscription/entitlement changes, bulk unit/owner imports. Back-office internal notes are access-controlled and never leak into ordinary building-member views. | §149.21 | none |

## Anti-patterns this work item must avoid (BRD §144, extended by §149's additions)

Do not duplicate a User per building; do not treat the `OWNER` role as the ownership
record; do not auto-activate a self-registered building without required back-office
approval; do not give onboarding/support agents permanent or unrestricted building
access; do not put subscription state directly into rental/finance records; do not use
entitlements as a substitute for building authorization.

## Explicitly deferred (unaffected milestones)

Rental/tenant/lease (BOS-003), payments (BOS-004), offline sync (BOS-005), work orders
(BOS-006), announcements (BOS-007), reporting/dashboards (BOS-008), security/release
hardening (BOS-009). Automated subscription billing and promotional/CRM features stay
deferred per §149.23/§125.

## Resolved — 2026-09-22

D-07: `PLATFORM_ADMIN` has full building-application approval authority, identical to
`SUPER_ADMIN`, no threshold. D-08: subscription plans are back-office admin-configured
data (plan CRUD in the console), not a hardcoded MVP catalog; at least one plan must
exist before a building application can be approved into an active subscription. See
DECISIONS.md. D-09/D-10 remain open as technical-design inputs for TECHNICAL.
