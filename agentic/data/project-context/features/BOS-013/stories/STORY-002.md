# STORY-002: Assign, invite and revoke building roles

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: L.

## Parent Epic
EPIC-BOS-013 ([EPIC.md](../EPIC.md))

## Objective
A Building Admin (and audited platform admins) can give an existing member an additional role,
invite a phone number to any assignable role, and revoke a role, generalizing the shipped F4 owner
invite → claim → revoke flow (§149.9 Step 5, §4.1 "Manage committee / managers / owners").

## Scope
- Generalize `InviteOwnerService` → invite-by-role (roles per Q-BOS-013-03; TENANT per
  Q-BOS-013-12), keeping one live invitation per building/phone/role (V5 unique index) and the
  existing TTL (Q-BOS-013-15).
- New use case: assign a role directly to a user who already has an active membership in the
  building (Q-BOS-013-05).
- Generalize revoke: `BuildingMembership.revoked()` today allows OWNER only; allow the roles
  Q-BOS-013-06 permits, with mandatory reason and `expectedVersion`.
- Optional committee `title` on assign/invite/claim (Q-BOS-013-02).
- `GET /members` gains `role` filter and returns `title`; `GET /me/building-invitations` returns
  role.
- Endpoints follow the existing `/api/v1/buildings/{buildingId}/invitations|members` shape with
  `operationId` idempotency (`building_operation`).

## Acceptance Criteria
1. An authorized assigner (Q-BOS-013-03) can invite a valid BD phone (`^01[3-9][0-9]{8}$`, V5) to
   COMMITTEE, PROPERTY_MANAGER, ACCOUNTANT, VIEWER, OWNER or TENANT; a second live invitation for
   the same building/phone/role is rejected as today.
2. Claim grants exactly the invited role to the caller whose verified JWT phone and subject match
   (ADR-F4-001 consequences); repeated claim with the same `operationId` returns the same result;
   expired or revoked invitations cannot be claimed.
3. Direct assignment to an existing active member creates a new ACTIVE role row; assigning a role
   the user already holds actively is rejected; assigning to a non-member is rejected (invite
   instead, Q-BOS-013-05).
4. A caller without role-administration permission (e.g. COMMITTEE, ACCOUNTANT, VIEWER, OWNER,
   TENANT) gets 403; PROPERTY_MANAGER may invite/revoke TENANT only if Q-BOS-013-03 confirms;
   nobody can grant or revoke their own role (BOS-010 ID-04 precedent).
5. BUILDING_ADMIN cannot be assigned, invited or revoked through these endpoints (platform-only,
   §4.1, STORY-006).
6. Every invite, claim, assign and revoke writes one `building_audit` row (actor, action, entity,
   reason, before/after) in the same transaction (§114 "Role changes").
7. No role change creates, modifies or ends an Ownership; revoking OWNER leaves ownership history
   intact (§5, §19.1, units-ownership "Membership versus ownership").
8. Writes on a SUSPENDED building are refused with the existing read-only error (`requireWritable`).
9. Tenant isolation: membership/invitation ids from building A used under building B return 404
   (cross-building object substitution test, ADR-F4-001 verification list).
10. Revoking the role removes it from the next building-context read (STORY-001 AC3), with
    revocation serialized against concurrent writes by the building lock (`BuildingAccess`).

## Tasks
| Lane | Proposed task | Size |
|---|---|---|
| BE/DB | Invite-by-role + claim generalization (migration for any invitation constraint changes, `title`) | M |
| BE | Direct assign use case + endpoint | S |
| BE | Revoke generalization + rules from Q-BOS-013-06 | S |
| BE | OpenAPI update for members/invitations | S |
| QA | Authorization matrix per assigner × target role, idempotency, expiry, substitution, audit rows | M |

## Dependencies
- STORY-001. Answers: Q-BOS-013-03 (blocking), Q-BOS-013-05, -06 (blocking for revoke),
  Q-BOS-013-12 (TENANT only), -15.
- BOS-003 STORY-009 consumes the TENANT invitation (Q-BOS-003-05).

## Out of Scope
- Bulk invitations (Q-BOS-013-14); BUILDING_ADMIN assignment (STORY-006); events (STORY-004);
  UI (STORY-005); automatic TENANT revoke on lease end (Q-BOS-013-12, BOS-003).

## References
BRD §4.1, §5, §19.1, §110, §114, §149.9 Steps 4–5; ADR-F4-001; UO-D04; BOS-002 D-04;
`InviteOwnerService`, `ClaimInvitationService`, `RevokeMembershipService`, V5 migration;
Q-BOS-003-04, -05; Q-BOS-007-04.
