# STORY-006: Platform assignment and removal of Building Admins

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M.

## Parent Epic
EPIC-BOS-013 ([EPIC.md](../EPIC.md))

## Objective
SUPER_ADMIN/PLATFORM_ADMIN can assign and remove Building Admins of an approved building from the
back-office console (§4.1 "Assign/remove Building Admins", §149.19
`POST /api/v1/platform/buildings/{buildingId}/assign-admin`), and can see a building's members and
roles. Today only the initial admin created at application approval (F2) exists.

## Scope
- building-service platform use cases: assign Building Admin (existing BuildingOS user, or by phone
  invitation if Q-BOS-013-03 allows), remove Building Admin with reason, last-admin protection
  (Q-BOS-013-06).
- back-office-service proxy (caller-token relay, existing pattern) and back-office web screen:
  building detail › Members (read-only list of roles, titles, status) + Assign/Remove admin.
- Emits `building.admin.assigned` / `building.admin.removed` via STORY-004 if approved
  (§149.20).

## Acceptance Criteria
1. Only SUPER_ADMIN and PLATFORM_ADMIN may assign/remove; ONBOARDING_AGENT, SUPPORT_AGENT,
   SUBSCRIPTION_ADMIN and every building role get 403 (§4.1; PLATFORM_ADMIN "Assign Building
   Admins"; removal by PLATFORM_ADMIN per Q-BOS-013-03).
2. Assigning creates an ACTIVE BUILDING_ADMIN membership; assigning an existing active admin is
   rejected; no ownership is created (§5).
3. Removing the last active Building Admin of an ACTIVE or ONBOARDING building is refused
   (§149.9 Step 11 prerequisite; Q-BOS-013-06).
4. Each assign/remove writes a `building_audit` row with platform actor, reason, before/after
   (§114) and appears in back-office Audit Logs aggregation (BOS-010 F6 BOC-08).
5. A platform admin cannot assign a Building Admin role to themselves (BOS-010 ID-04 precedent).
6. Members view shows only the selected building's memberships (§110); phone numbers masked per
   existing back-office rules (§94).

## Tasks
| Lane | Proposed task | Size |
|---|---|---|
| BE | building-service platform assign/remove admin use cases + last-admin rule | M |
| BE | back-office-service proxy endpoints | S |
| Web | Back-office building Members tab + assign/remove dialogs | S |
| QA | Platform-role matrix, last-admin, audit aggregation, isolation | S |

## Dependencies
- STORY-001; STORY-002 shares revoke rules; STORY-004 for events (optional).
- Answers: Q-BOS-013-03 (PLATFORM_ADMIN removal; assign by phone), Q-BOS-013-06 (blocking).

## Out of Scope
- Platform role management (BOC-05, delivered); onboarding-agent assignment (§149.19 separate
  endpoint).

## References
BRD §4.1, §5, §94, §110, §114, §149.9 Step 11, §149.19, §149.20; BOS-010 back-office-console
REQUIREMENTS (BOC-05, BOC-08), TECH-SPEC (ID-04); ADR-F4-001.
