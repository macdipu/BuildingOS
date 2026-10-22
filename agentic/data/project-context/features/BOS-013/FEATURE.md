# BOS-013 intake — Building roles, role assignment and membership/unit events

Status: PROPOSED (planning draft 2026-10-22; not approved). Intake mode: prompt-first.
Raised 2026-10-22 by planning as a **cross-epic dependency**; no operator request text beyond the
planning prompt. Nothing here is a decision.

## Request (normalized)
Six parallel epic plans (BOS-003 rental, BOS-004 finance, BOS-006 work, BOS-007 communication,
BOS-008 reporting, BOS-011 per-role nav) each found the same gap and each deferred it to "a
membership/roles item":

- building-service models only `BuildingRole { BUILDING_ADMIN, OWNER }` (enum + V5
  `membership_role_check`, invitation `role = 'OWNER'`), while BRD §4.1 defines COMMITTEE,
  PROPERTY_MANAGER / RENT_MANAGER, TENANT, ACCOUNTANT, VIEWER / AUDITOR and §149.9 Step 5 lets a
  Building Admin invite/assign them;
- new services need a service-to-service membership/role (and unit/ownership) lookup — today only
  `GET /internal/directory/buildings/{id}` exists (D-38, scope `directory.read`);
- building-service publishes only `ownership.transferred`; BRD §8.2 lists `unit.created`,
  `unit.updated`, `ownership.assigned`, §8.1 lists `building.member.added/removed`, `role.assigned`,
  §150.8 expects "membership granted / revoked, role changed";
- Flutter has per-role nav (BOS-011 D-02) but Committee/Tenant sets are unreachable and there is no
  role-assignment UI (only owner invite/revoke in `members_page.dart`).

## Classification (proposed — run `work-item-level-classifier` before leaving TECHNICAL)
- Work type: FEATURE / CR on delivered BOS-010 F4 membership (brownfield, building-service +
  auth-service + user_app + back-office web).
- Proposed level: EPIC with 6 stories ([EPIC.md](EPIC.md)).
- Project type: brownfield (`agentic/kit/workflows/brownfield.md`).

## Inputs consolidated
Q-BOS-003-04, -05, -08; Q-BOS-004-02, -03 (role part), -31; Q-BOS-006-02; Q-BOS-007-03, -04, -23
(membership part); Q-BOS008-02 (building-service part), -04, -06 (role list part); BOS-011
DECISIONS "Tenant and Committee nav sets unreachable"; BOS-002 D-04. Mapping in
[OPEN-QUESTIONS.md](OPEN-QUESTIONS.md).

## Constraints carried forward
- ADR-F4-001 (ACCEPTED 2026-09-24): building-service is the sole building-membership/invitation
  authority (explicit deviation from BRD §8.1/§9); JWTs do not carry building roles.
- BOS-010 UO-D01: only Building Admins (and audited platform admins) edit units/ownership.
- D-38: service JWT only for listed internal existence lookups; extending it needs approval.
