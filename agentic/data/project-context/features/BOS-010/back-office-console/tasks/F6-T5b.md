# F6-T5b..T5d — Audit Logs (BOC-08, slices of F6-T5)

## Category
BE

## Objective
System > Audit Logs reads every service's own audit records through one back-office API,
without copying them (TECH-SPEC "Audit aggregation": each service stays the source of truth).

## Common internal read contract (every service)
`GET /internal/audit?since=&until=&entityType=&actorUserId=&limit=` — relayed bearer token,
the service's own JWT check, `SUPER_ADMIN` only; newest first; `limit` default 50, max 200.
Item: `{ id, source, occurredAt, actorUserId (nullable = system), action, entityType, entityId,
buildingId (nullable), reason (nullable) }`. `before`/`after` JSON payloads are **not** returned
(no sensitive-data leak into the admin list).

## F6-T5b — auth-service platform-role audit (D-37)
- Flyway: `platform_role_audit (id, actor_user_id, target_user_id, role, action GRANT|REVOKE, occurred_at)`.
- Assign/revoke use cases write one row in the same transaction as the role change.
- `/internal/audit` over it (`entityType` = `PLATFORM_ROLE`, `entityId` = target user).
- Tests + `mvn verify` green for auth-service.

## F6-T5c — building-service + subscription-service internal audit reads
- building-service: `lifecycle_transition` (action = `from→to`) + `building_audit` merged.
- subscription-service: `audit_event`.
- Read-only; no change to how rows are written. Tests + `mvn verify` green for both.

## F6-T5d — back-office aggregation
- `back-office-service` own `lifecycle_transition` + fan-out to the three services, merged
  newest first, truncated to `limit`; response `{ items, nextUntil }` (cursor = oldest
  `occurredAt` returned). A failed source → listed in `unavailableSources`, others still shown.
- `GET /api/v1/platform/backoffice/audit?since=&until=&source=&entityType=&actorUserId=&limit=`,
  `SUPER_ADMIN` only; OpenAPI entries for all new endpoints.

## Out of Scope
Backfill of pre-F6-T5b role changes; audit writing changes in other services; UI (F6-T9).

## References
../TECH-SPEC.md; REQUIREMENTS.md BOC-05, BOC-08; DECISIONS D-37; AU-01.

## Status
F6-T5b DONE (auth-service 93 tests green, contract check passed): V4 platform_role_audit, row only on real state change, same transaction as grant/revoke; action PLATFORM_ROLE_GRANTED/REVOKED; bad since/until/limit → 400; since>until → empty. F6-T5c DONE (building-service 125, subscription-service 37 tests green): building action FROM→TO (NONE on creation) for lifecycle_transition, stored action for building_audit; buildingId set for BUILDING entities and building_audit; subscription entityId string. F6-T5d PLANNED.
