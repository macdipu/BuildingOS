# UI-T06 — Unit list owner filter, search, sort

## Category
BE

## Objective
Extend units list query for BRD §48 Owner filter, Search, Sort.

## Scope
building-service `unit/presentation/rest/UnitController`, `unit/application/listunits/*`, repository; `contracts/openapi/platform.yaml`.

## Dependencies
None

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- `ownerUserId` returns units with an active ownership by that user.
- `q` matches unit number case-insensitively (contains).
- `sort` accepts unitNumber|floor|type with asc|desc; invalid -> 400.
- Omitting new params = current behavior; pagination and authorization unchanged.

## Test Requirements
Use-case unit tests; Testcontainers integration test per param; ArchUnit green; `mvn -pl building-service verify`.

## UI Reference
Serves `ui/stitch/mobile/10-unit-list`; BRD §48, §117 search, §118 pagination.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
Occupancy/due-status filters (BOS-003/004).
