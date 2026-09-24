# F4-T4a — Ownership assignment, transfer and history

## Category
BE (building-service)

## Objective
Co-ownership allocations, partial/full transfers and protected history (UO-05/06/07/10)
under the immediate-date branch (UO-D02) with transactional audit and outbox (F4-T5 rule).

## Scope
- V8: `ownership_period`, `ownership_transfer`, `building_outbox`,
  `building_unit.ownership_revision`.
- `POST .../units/{u}/ownerships`: ownerUserId, share, effectiveDate, notes, reason,
  expectedVersion (unit ownership revision), operationId.
- `POST .../units/{u}/ownership-transfers`: sourceOwnerUserId, recipientUserId, share,
  effectiveDate, optional reference, reason, expectedVersion, operationId.
- `GET .../units/{u}/ownership-history`: admins see every period and transfer; an owner
  with active membership sees only their own periods/transfers (UO-08, history policy).
- Transfers write `ownership.transferred` v1 to `building_outbox` in the same transaction.

## Rules (approved)
Shares: exact, >0, ≤100, at most 4 decimals, never rounded. Current total ≤100; 100% is not
forced. Effective date must be today in Asia/Dhaka (`buildingos.ownership.zone`); the change
takes effect at the server instant and an ordered unit revision, so same-day changes keep
order. A transfer closes the source's period and opens its remainder, and closes/reopens the
recipient's existing allocation with the sum; only affected allocations change. Owners must
hold an active OWNER membership (ownership only after claim). No lease/tenant writes, no
generic PUT/DELETE. Building lock then unit lock; SUSPENDED is read-only.

## Implementation notes
An assignment to someone who already holds an open allocation on the unit is rejected
(ALREADY_OWNER); changing an existing owner's share goes through a transfer. No event is
emitted for assignment: the approved contract defines only `ownership.transferred`.

## Out of Scope
Owner-filtered unit reads, My Properties, ownedUnitCount (F4-T4b); Kafka publisher and
transfer documents (F4-T5); gateway/OpenAPI (F4-T6).

## Status
COMPLETED 2026-09-24 (run RUN-C73FA516545143FAA9B35B168EEFE0C9). UnitOwnershipTest 3 +
OwnershipApiIntegrationTest 5; event contract contracts/kafka/ownership-transferred.v1.schema.json
(check-contracts passed); building-service verify 97/0, ArchUnit 7/0, platform-web 5/0.
Outbox rows stay pending until the F4-T5 publisher.
