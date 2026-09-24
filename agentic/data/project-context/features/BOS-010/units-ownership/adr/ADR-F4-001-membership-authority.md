# ADR-F4-001 — Single authority for building membership

Status: ACCEPTED by operator, 2026-09-24. Decision: keep building-service as the sole
building-membership/invitation authority; auth-service remains global identity.
Date: 2026-09-24. Scope: BOS-010 F4 UO-01/05/08/09.

## Context

BRD §8.1 assigns BuildingMembership and Invitation to Identity. F2 already persists
initial BUILDING_ADMIN memberships in building_db. ApproveApplicationService creates
the building, first membership and lifecycle transitions in one local transaction;
ActivateBuildingService counts active admins in the same database.

F4 introduces current-membership checks, phone invitations, owner roles and revocation.
Duplicating authoritative membership state across services would make revocation and
data access ambiguous. The user has not approved a source-of-truth change.

## Proposed decision

Keep building-service as the sole authority for building memberships and invitations
for F4, extending the shipped F2 table behind a dedicated membership feature/port.
auth-service remains the sole authority for global users, verified phone identity
and platform roles. Resolve identity from its validated JWT when invitations are claimed.

Explicit BRD deviation: building membership/invitation storage belongs to building-service,
not auth-service, for this implementation. Update architectural ownership only after
technical approval of this proposal. Never claim that the existing BRD already says this.

## Alternatives

1. Move authority into auth-service now. Aligns with BRD §8.1, but requires migration
   of F2 initial admins, a new authorized membership API, compatibility sequencing,
   and a recoverable multi-service approval workflow replacing the current atomic
   building/admin transaction. A remote permission check also needs precise concurrent
   revocation semantics. Viable if those changes are explicitly selected and designed.
2. Keep two writable authorities. Rejected: conflicting grants/revocations, split
   audit ownership, stale reads and hard-to-prove access guarantees.
3. Keep only building-service authoritative (proposed). No cross-database join or
   membership migration. Building state, access revocation and unit writes serialize
   locally. Future services consume an explicit authorization contract when needed.

## Consequences

- F2 creation and activation keep their existing local consistency boundary.
- Invitation creation requires an authorized building admin or platform administrator;
  claim matches a canonical JWT phone and subject. The existing platform-only
  /internal/users/provision endpoint remains restricted.
- No owner record implicitly grants membership. No membership implicitly grants ownership.
- Database records decide building access on each request; JWTs do not carry building roles.
- Building-service grows a membership feature. A later move to auth requires a new ADR,
  migration and rollout design; it cannot merely copy records and switch readers.
- Conservative locking can serialize writes within a building; optimize only with
  evidence while preserving revocation ordering.

## Approval and verification

Approval requested as part of the complete F4 technical proposal, separately from
F2's completed release. Required checks: retained F2 initial admin, invitation claim
by the intended verified identity only, repeated claim, expired/revoked invitation,
membership revoke versus mutation ordering, and cross-building object substitution.

If the operator chooses auth-service authority instead, revise this ADR and dependent
design before implementation; no gate is satisfied by this document's existence.


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
