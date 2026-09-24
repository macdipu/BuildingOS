# F4-T4b — Owner-filtered reads, My Properties and owned-unit counts

## Category
BE (building-service)

## Objective
Owners see only their real current allocations (UO-01/08, UO-D01), completing F4-T4.

## Scope
- `GET .../units` and `GET .../units/{u}`: building/platform admins see all units; an OWNER
  member sees only units they currently own (others look missing).
- `GET .../units/{u}/ownerships`: current allocations + ownership revision (the
  `expectedVersion` for writes). Admins see every allocation; an owner sees only their own.
- `GET /api/v1/me/properties`: caller's current allocations in buildings where they hold an
  active membership (unit, floor, type, area, building, share, since). Paginated.
- `GET /api/v1/me/buildings` gains `ownedUnitCount`.

## Acceptance Criteria
- An owner of three units sees only those; another unit's ID returns 404 for detail and
  allocations (REQUIREMENTS example).
- Co-owners' allocations are not exposed to an owner.
- Revoked membership removes the building from My Properties/My Buildings.

## Decision recorded
F4-T5 will use spring-kafka for the outbox publisher (operator, chat 2026-09-24).

## Status
COMPLETED 2026-09-24 (run RUN-C73FA516545143FAA9B35B168EEFE0C9). OwnerReadsIntegrationTest 3; the F4-T2 owner
assertion now expects a filtered list instead of 403. building-service verify 100/0, ArchUnit 7/0, platform-web 5/0.
F4-T4 is complete.
