# F4-T1b — Membership and invitation use cases

## Category
BE (building-service)

## Objective
Complete F4-T1: building-service as the single membership/invitation authority
(ADR-F4-001) with authoritative per-request access checks.

## Scope
- `BuildingAccess`: locks the building row (FOR UPDATE for writes, FOR SHARE for reads)
  and resolves the caller's current platform/membership role inside that transaction.
  Hidden buildings → 404, known building without the role → 403.
- Admin: invite OWNER by phone, list invitations, revoke invitation, list members,
  revoke OWNER membership (reason + expectedVersion).
- Recipient: list own pending invitations and claim one by JWT `sub` + `phone`; claim
  takes an operationId (idempotent, fingerprinted, conflict on changed payload).
- Audit row for every mutation in the same transaction.
- SUSPENDED buildings are read-only (DECISIONS "Building status").

## Implementation notes
Invitation expiry is the configured TTL (`buildingos.membership.invitation-ttl`, default
P7D per LLD); the client cannot set it. A duplicate live invitation returns the existing
one (200) without a new audit row. Time-expired PENDING invitations are reported as
EXPIRED and transitioned under the building lock before a replacement is created.
A claim by a subject whose revoked OWNER row exists reinstates that row (unique
building/user/role); revocation history remains in `building_audit`.

## Acceptance Criteria
- Only the intended verified phone can see/claim; repeated claim by the same subject
  returns the same membership; other subjects are rejected.
- Expired/revoked invitations cannot be claimed; revoked membership needs a new invite.
- Revocation and mutations serialize on the building lock.
- Cross-building IDs return 404; non-admin members get 403; F2 regression passes.

## Test Requirements
Spring Boot + Testcontainers API integration tests; backend Maven verify; ArchUnit unchanged.

## References
../TECH-SPEC.md (Invitation and identity binding, Idempotency, Mutation transaction);
UO-01/05/08/10; ADR-F4-001.

## Out of Scope
My Buildings / building context (F4-T2), gateway routes/OpenAPI (F4-T6), mobile, outbox.

## Status
COMPLETED 2026-09-24 (run RUN-C73FA516545143FAA9B35B168EEFE0C9). 7 API integration tests
(MembershipApiIntegrationTest); building-service verify 67/0, ArchUnit 7/0, platform-web 5/0.
F4-T1 is complete. Gateway forwarding of these paths is F4-T6.
