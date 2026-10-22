# STORY-005: Mobile members & roles screen and per-role navigation

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M.

## Parent Epic
EPIC-BOS-013 ([EPIC.md](../EPIC.md))

## Objective
A Building Admin manages members and roles from the app (§89 Settings › Access: Members, Roles,
Permissions), invitees see the role they are invited to, and users holding the new roles get the
§37 navigation set for their role (BOS-011 D-02).

## Scope
- Extend `members_page.dart` (units_ownership feature) or a new `building_access` feature:
  member list with role filter and committee title; actions Invite (phone + role + optional title),
  Add role (existing member), Revoke role (reason dialog, as today for owners); pending invitations
  with revoke.
- Read-only "Roles" view listing roles and their permissions from building context (STORY-001).
- Pending invitations (§42 "Pending Invitations", `Accept Invitation`) show the role.
- `nav_sets.dart`: map COMMITTEE, PROPERTY_MANAGER, TENANT, ACCOUNTANT, VIEWER per Q-BOS-013-13;
  remove the "unmapped role → tenant set" fallback for known roles.
- Actions shown from context permissions, never from role names in widgets (§5); the backend still
  re-authorizes (`BuildingContext` comment, UO-01).

## Acceptance Criteria
1. A BUILDING_ADMIN sees Members with role filter, invitations, and Invite/Add role/Revoke actions;
   a user without role-administration permission sees the list read-only or not at all per
   Q-BOS-013-03 and never sees mutation actions.
2. Invite validates the BD phone format before submit and shows backend errors (duplicate live
   invitation, invalid role, read-only building) with the existing error mapping.
3. Revoke requires a non-empty reason and sends `expectedVersion`; a stale version shows "changed,
   reload" (`STALE_VERSION`).
4. BUILDING_ADMIN rows show no Revoke action (platform-only, STORY-006).
5. Committee title is shown as a label only and drives no UI permission (§149.9 Step 5).
6. Nav: COMMITTEE → Committee set, TENANT → Tenant set, PROPERTY_MANAGER/RENT_MANAGER → manager set,
   ACCOUNTANT/VIEWER per Q-BOS-013-13; multi-role precedence per BOS-011 Q-01 extended by
   Q-BOS-013-13; widget tests per role and per combination.
7. Switching active building re-reads roles and rebuilds nav; roles of building A never affect
   building B's nav (§110).
8. Strings via `TextEnum` (en/bn as existing), layout per DESIGN.md tokens; no mockup-only elements
   (BOS-011 D-03).

## Tasks
| Lane | Proposed task | Size |
|---|---|---|
| Mobile | Data/domain: role invite, assign, revoke use cases + models (roles, permissions, title) | S |
| Mobile | Members & roles UI (list, filter, invite/assign/revoke dialogs, roles view) | M |
| Mobile | Nav mapping for new roles + precedence update | S |
| QA | Widget tests per role/combination; integration against STORY-002 endpoints | S |

## Dependencies
- STORY-001, STORY-002. Answers: Q-BOS-013-13 (blocking for nav), Q-BOS-013-03 (action
  visibility), Q-BOS-013-02.

## Out of Scope
- Role dashboards and module tabs' content (BOS-008 and module epics); bulk invite
  (Q-BOS-013-14); owner directory financials (§53, BOS-003/004/008).

## References
BRD §5, §37, §42, §89, §110, §149.9 Step 5; BOS-011 D-02, D-03, Q-01; UI-INDEX C-1;
`nav_sets.dart`, `app_shell_controller.dart`, `members_page.dart`; Q-BOS008-06.
