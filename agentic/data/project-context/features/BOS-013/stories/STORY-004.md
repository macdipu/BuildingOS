# STORY-004: Building-service unit, ownership and membership events

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: L.

## Parent Epic
EPIC-BOS-013 ([EPIC.md](../EPIC.md))

## Objective
Publish the building-domain events other services consume (§8.3/§8.4 consume `unit.created`;
§150.8 consumes membership and ownership events; reporting projections §8.9) through the existing
building-service outbox, so consumers can keep projections instead of polling.

## Scope
- Unit events (§8.2): `unit.created`, `unit.updated` from create / batch create / update use cases.
- Ownership event (§8.2): `ownership.assigned` from initial ownership assignment; existing
  `ownership.transferred` unchanged in payload.
- Membership events per Q-BOS-013-09 (default: `building.member.added`, `building.member.removed`,
  `role.assigned`, proposed `role.revoked`; plus §149.20 `building.admin.assigned/removed`,
  `owner.invited`, `owner.invitation.accepted` if approved).
- Topic convention per Q-BOS-013-10 (default `building.events`, event type in envelope/header);
  `OutboxProperties.topics` updated accordingly.
- JSON schemas `contracts/kafka/<event>.v1.schema.json`, all using `event-envelope.schema.json`.

## Acceptance Criteria
1. Each listed event is inserted into `building_outbox` in the same DB transaction as the business
   change; a rolled-back change emits nothing (§15).
2. Every event carries unique `eventId`, `eventType`, `eventVersion=1`, `occurredAt`, `producer =
   building-service`, `buildingId`, `correlationId` (§14.1 rules 4–7) and validates against its
   schema in a test.
3. Batch unit creation emits one `unit.created` per unit created; a no-op update emits no
   `unit.updated`.
4. `ownership.assigned` carries unit id, owner user id, share percentage, valid_from (§19.1); a
   transfer still emits only `ownership.transferred` (no double event).
5. Granting a user their first active role in a building emits `building.member.added` and
   `role.assigned`; revoking their last active role emits `role.revoked` (if approved) and
   `building.member.removed`; intermediate grants/revokes emit only the role event
   (Q-BOS-013-09).
6. Payloads contain ids, role, title, status, versions and timestamps only — no phone, name or NID
   (§94, Q-BOS-013-11).
7. Events of building A carry only building A's `buildingId`; a consumer-side idempotency test
   replays the same `eventId` without double effect (§14.1 rule 3 guidance for consumers).
8. Publisher failures are retried by the existing outbox job with backoff; backlog metrics
   (`buildingos.outbox.pending`) cover the new events.

## Tasks
| Lane | Proposed task | Size |
|---|---|---|
| BE | Unit events from unit use cases (+ batch) | M |
| BE | `ownership.assigned` from ownership assignment | S |
| BE | Membership/role events from STORY-002 use cases (and F4 owner flows) | M |
| BE/DB | Topic convention change + config (and `ownership.transferred` migration if approved) | S |
| BE | Kafka JSON schemas + schema validation tests | S |
| QA | Transactional outbox tests, schema tests, ordering per aggregate, no-PII check | M |

## Dependencies
- STORY-001; STORY-002 for the role-event half (unit/ownership half can ship first).
- Answers: Q-BOS-013-09 (blocking for membership events), Q-BOS-013-10 (blocking for all), -11.
- Consolidates Q-BOS008-02 (building-service part), Q-BOS-007-03 (b), Q-BOS008-04 (b).

## Out of Scope
- Consumers in other services; `building.created/updated/activated/suspended` (§8.2) unless the
  operator adds them; historical replay (Q-BOS-013-11 default).

## References
BRD §8.1, §8.2, §8.3/§8.4 consumes, §14.1, §14.2, §15, §19.1, §94, §149.20, §150.8; ADR-F4-001;
`TransferOwnershipService.EVENT_TYPE`, `OutboxProperties`, `contracts/kafka/`.
