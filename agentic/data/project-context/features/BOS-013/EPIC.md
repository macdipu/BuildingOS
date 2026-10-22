# EPIC-BOS-013: Building roles, role assignment and membership/unit events

Status: PROPOSED (planning draft 2026-10-22; not approved)

No dates, hours or capacity are implied. Sizes are relative (S/M/L/XL). Every rule cites the BRD
(`agentic/data/project-context/BuildingOS_BRD_Agentic_Development.md`) or a decision file;
everything the BRD leaves open is in [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) (Q-BOS-013-NN) with a
recommended default that is **not** a decision. Intake: [FEATURE.md](FEATURE.md).

## Objective

Make building-service the complete source of building roles that every other epic depends on:
add the §4.1 building roles, let authorized people assign and revoke them (with audit), expose
roles/permissions, units and ownership to other services through an internal service-authenticated
lookup, publish the building and membership events BRD §8.1/§8.2 name, and give building admins a
role-assignment UI with per-role navigation.

## Business Context

- BRD §3.1 lists "Role assignment" in scope; §123 Phase 1 lists "Building memberships and building
  roles" and "Invitations".
- §4.1 defines building roles BUILDING_ADMIN, COMMITTEE, OWNER, PROPERTY_MANAGER / RENT_MANAGER,
  TENANT, ACCOUNTANT, VIEWER / AUDITOR. §5: a user holds different roles per building, ownership is
  separate from the OWNER role, and authorization uses permission checks, not role names.
- §149.9 Step 5: a Building Admin may invite/assign Committee, Property/Rent Manager, Accountant,
  Viewer/Auditor; human titles (President, Secretary, Treasurer) are metadata only.
- §114: role changes are audited. §110: every query is scoped by `building_id` and membership.
- Delivered today (BOS-010 F2/F4, ADR-F4-001 ACCEPTED): building-service is the sole membership and
  invitation authority; `BuildingRole { BUILDING_ADMIN, OWNER }`; owner invite → claim → revoke with
  `building_audit`; outbox publishing only `ownership.transferred`; one internal endpoint
  `GET /internal/directory/buildings/{id}` behind a service JWT with scope `directory.read` (D-38).
- Five planned epics are blocked or degraded by this gap (BOS-003, -004, -006, -007, -008) and
  BOS-011's Committee/Tenant nav sets are unreachable. Mapping of their questions:
  [OPEN-QUESTIONS.md — Upstream questions](OPEN-QUESTIONS.md#upstream-questions-this-epic-answers-or-consolidates).

## Scope

| Area | Owner | Change |
|---|---|---|
| Building role catalogue + permission catalogue | building-service, `contracts/` | Extend `BuildingRole`, DB checks, role→permission contract (STORY-001) |
| Role invitation, direct assignment, revocation, committee title | building-service | Generalize F4 invite/claim/revoke to all roles (STORY-002) |
| Internal membership / unit / ownership lookup | building-service, auth-service (service clients) | New `/internal/directory/...` reads, new scope (STORY-003) |
| Building + membership domain events | building-service outbox, `contracts/kafka` | `unit.created`, `unit.updated`, `ownership.assigned`, membership/role events (STORY-004) |
| Role assignment UI + per-role nav | user_app (Flutter) | Members & roles under Settings › Access (§89), nav for new roles (STORY-005) |
| Platform Building Admin assignment | building-service, back-office web | Assign/remove Building Admin, members view (STORY-006) |

## Out of Scope

- Moving membership authority to auth-service (would need a new ADR — ADR-F4-001 consequences).
- Tenant profiles, leases, rental use of TENANT/PROPERTY_MANAGER (BOS-003); finance, work,
  communication and reporting authorization inside those services (BOS-004/006/007/008) — they
  consume STORY-001/003/004.
- Admin-configurable custom roles (Q-BOS-013-01), bulk invitations (Q-BOS-013-14), role dashboards
  (BOS-008), JWT role/permission claims (ADR-F4-001: database records decide access per request).
- Platform roles (BOS-010 back-office BOC-05, delivered).

## Stories

| Story | Title | Size | Depends on |
|---|---|---|---|
| [STORY-001](stories/STORY-001.md) | Building role catalogue and permission catalogue | M | — |
| [STORY-002](stories/STORY-002.md) | Assign, invite and revoke building roles | L | 001 |
| [STORY-003](stories/STORY-003.md) | Internal membership, unit and ownership lookup for services | M | 001 |
| [STORY-004](stories/STORY-004.md) | Building-service unit, ownership and membership events | L | 001 (002 for role events) |
| [STORY-005](stories/STORY-005.md) | Mobile members & roles screen and per-role navigation | M | 001, 002 |
| [STORY-006](stories/STORY-006.md) | Platform assignment/removal of Building Admins | M | 001 (002 for shared revoke rules) |

Total relative size: 2×L + 4×M (≈ XL overall).

## Cross-Story Dependencies

```text
STORY-001 ─┬─▶ STORY-002 ─┬─▶ STORY-005
           │              └─▶ STORY-004 (role/membership events)
           ├─▶ STORY-003            (unit/ownership events need only 001)
           └─▶ STORY-006
```

Each story is shippable alone after STORY-001: STORY-003 and the unit/ownership half of STORY-004
need no new roles to be useful. Downstream epics: BOS-003/004/006/007/008 authorization adapters
need STORY-001 + STORY-003; BOS-007 chat and BOS-008 projections need STORY-004.

## Acceptance Criteria

1. building-service accepts and stores the §4.1 building roles agreed in Q-BOS-013-01; a user may
   hold several roles in one building and different roles in different buildings (§5, §110.1).
2. Every role grant and revocation is authorized per Q-BOS-013-03, refused on SUSPENDED buildings,
   audited in `building_audit` with actor, reason, before/after (§114), and never creates or
   removes ownership (§5, ADR-F4-001).
3. Other services can resolve a user's active roles/permissions in a building, list members by
   role, and read a unit with its current ownership through internal endpoints that accept only a
   service JWT with the approved scope (Q-BOS-013-07); user tokens get 403.
4. building-service publishes the events agreed in Q-BOS-013-09/10 through its outbox in the same
   transaction as the change, with the §14.1 envelope, and JSON schemas under `contracts/kafka/`.
5. A Building Admin can assign/revoke roles from the app; users with the new roles see the §37 nav
   set for their role (BOS-011 D-02, Q-BOS-013-13).
6. Tenant isolation: no endpoint, lookup or event leaks memberships, units or ownership of building
   A to a caller scoped to building B (§110); covered by integration tests in every story.

## Risks

1. **Authorization blast radius**: widening `BuildingRole` touches `BuildingAccess`, which guards
   every building-service write. A mistaken `isAdmin()`/role check could grant COMMITTEE or VIEWER
   write access. Mitigation: STORY-001 keeps admin-only write paths unchanged and adds negative
   tests per new role.
2. **Unconfirmed permission matrix**: §4.1/§138/§150.10 do not cover every role × permission
   (Q-BOS-013-04). Shipping roles before the matrix is confirmed lets each downstream epic diverge.
3. **D-38 amendment and new service clients**: the internal lookup widens service-credential use
   from one caller to up to six; secrets, rotation and scope checks multiply (Q-BOS-013-07).
4. Event contract churn: topic convention and membership event names (Q-BOS-013-09/10) must be
   fixed before any consumer ships, or consumers need migrations.
5. Revocation latency across services (Q-BOS-013-08) is a data-access risk for chat and finance.

## References

- BRD §3.1, §4.1, §5, §8.1, §8.2, §8.3/§8.4 (consume `unit.created`), §9, §14–§15, §19.1, §37,
  §89, §94, §104, §106, §110, §114, §123, §137, §138, §149.9 (Steps 4, 5, 9, 11), §149.19, §149.20,
  §150.8, §150.10.
- Decisions: `features/BOS-010/units-ownership/adr/ADR-F4-001-membership-authority.md`,
  `features/BOS-010/units-ownership/DECISIONS.md` (UO-D01, UO-D04), `features/BOS-010/DECISIONS.md`
  (D-38), `features/BOS-002/DECISIONS.md` (D-04), `features/BOS-011/DECISIONS.md` (D-02, Q-01).
- Upstream plans: `features/BOS-003|004|006|007|008/EPIC.md` and `OPEN-QUESTIONS.md`.
- Code: `backend/building-service/.../building/domain/model/BuildingRole.java`,
  `.../membership/application/BuildingAccess.java`, `.../membership/presentation/rest/`
  (`BuildingMembershipController`, `MyInvitationController`),
  `.../building/presentation/rest/DirectoryBuildingController.java`, migrations V4/V5,
  `.../outbox/`, `contracts/kafka/`, `user_app/lib/app/shell/nav_sets.dart`,
  `user_app/lib/features/units_ownership/presentation/pages/members_page.dart`.
