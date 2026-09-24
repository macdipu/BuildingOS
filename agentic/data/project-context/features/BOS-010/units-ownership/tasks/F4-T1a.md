# F4-T1a — Membership and invitation persistence foundation

## Category
DB/Integration

## Objective
Add the forward migration needed for OWNER membership, phone invitations, audit and
idempotent operations while retaining F2 initial BUILDING_ADMIN records.

## Scope
V5 schema; compatible role/status enum additions; isolated PostgreSQL migration tests.
No public membership/invitation API or access grant is enabled by this task.

## Dependencies
Operator-approved F4 packet and recorded technical gate; existing V1–V4 migrations.

## Implementation Requirements
Preserve membership IDs and original createdAt on upgrade. Version/update/revocation
fields must have valid backfills. Constrain canonical invite phone, OWNER-only grants,
single pending invite, state metadata and identity binding. Store audit and idempotency
metadata transactionally with future use cases. Keep per-building foreign keys.

## Acceptance Criteria
- V4 data upgrades without losing its admin or timestamps; rerunning migrations is safe.
- Fresh installs migrate through V5.
- Invalid states, duplicate pending invites, duplicate operation IDs and foreign
  building references are rejected by PostgreSQL.
- Historical terminal invites permit a new pending invitation.
- F2 regression tests continue passing.

## Test Requirements
Migration integration tests on real Testcontainers Postgres and backend Maven verify.
No production/local persistent database migration.

## References
../TECH-SPEC.md; UO-01/05/08/10; ADR-F4-001.

## Out of Scope
HTTP authorization/use cases, unit/ownership tables, mobile screens and deployment.

## Status
COMPLETED 2026-09-24. V5 migration + 6 Testcontainers migration tests; building-service
verify 60/0, platform-web 5/0. Remaining F4 work must not be represented as complete by this subtask.
