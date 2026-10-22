# STORY-001: Building role catalogue and permission catalogue

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M.

## Parent Epic
EPIC-BOS-013 ([EPIC.md](../EPIC.md))

## Objective
building-service can store and return every §4.1 building role and the permissions each role
holds, so other epics authorize by permission (§5) instead of by the two roles that exist today.

## Scope
- Extend `BuildingRole` with the roles agreed in Q-BOS-013-01 (default `COMMITTEE`,
  `PROPERTY_MANAGER`, `TENANT`, `ACCOUNTANT`, `VIEWER`; RENT_MANAGER/AUDITOR as labels).
- Flyway migration widening `membership_role_check` and the invitation `role` check (V5); add
  nullable `title` on membership for COMMITTEE (Q-BOS-013-02).
- Versioned role→permission catalogue contract (Q-BOS-013-04), seeded only from §137, §138,
  §150.10; unconfirmed cells flagged PROPOSED.
- Building context (`GET /api/v1/buildings/{b}`, `/api/v1/me/buildings`) returns `roles[]` and
  `permissions[]` for the caller; `canManageMembers` derived from the role-administration
  permission (Q-BOS-013-03) instead of `isAdmin()` alone.
- `BuildingAccess` keeps write paths that are BUILDING_ADMIN-only today exactly as they are
  (UO-D01: other roles get no unit/ownership-edit authority).

## Acceptance Criteria
1. A membership row with any catalogue role can be persisted; any other value is rejected by the DB
   check and the domain (§4.1, Q-BOS-013-01).
2. A user can hold several roles in the same building (one row per role, V4 `UNIQUE(building_id,
   user_id, role)`) and different roles in different buildings (§5, §110.1); building context lists
   all active roles.
3. The context response contains the union of permissions of the caller's active roles from the
   catalogue; a REVOKED role contributes none (§5 "permission checks").
4. Authorization regression: a member holding only COMMITTEE, PROPERTY_MANAGER, TENANT, ACCOUNTANT
   or VIEWER gets 403 on every existing unit, floor, ownership, invitation and membership write
   (UO-D01); a non-member gets 404 as today (`BuildingAccess` "reported as missing"). One
   parameterized test per role.
5. Tenant isolation: roles held in building A are never included in the context for building B
   (§110).
6. Existing BUILDING_ADMIN and OWNER memberships, invitations and audit rows are unchanged by the
   migration (verified on a migrated F4 dataset).
7. The catalogue file is versioned, lives under `contracts/`, and a test fails if code and contract
   disagree.

## Tasks
| Lane | Proposed task | Size |
|---|---|---|
| BE/DB | Enum + migration (role checks, `title` column) + domain validation | S |
| BE | Permission catalogue contract + loader + context response `roles`/`permissions` | M |
| BE | `BuildingAccess`/`BuildingContext` regression for new roles | S |
| QA | Per-role negative authorization matrix, migration-on-F4-data test, isolation test | S |

## Dependencies
- Answers needed: Q-BOS-013-01 (blocking), Q-BOS-013-04 (catalogue content; can ship with
  PROPOSED cells), Q-BOS-013-02 (title column only).

## Out of Scope
- Granting the new roles (STORY-002); consumers' enforcement (BOS-003/004/006/007/008).

## References
BRD §4.1, §5, §110.1, §137, §138, §150.10; ADR-F4-001; UO-D01; BOS-002 D-04; Q-BOS-004-02,
Q-BOS-007-04, Q-BOS-003-04.
