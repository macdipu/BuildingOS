# STORY-001: Reporting-service foundation, unit/ownership projection, dashboard frame

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Stand up the reporting-service with an idempotent Kafka projection pipeline, project the
building/unit/ownership data that already exists (BOS-010), and replace the placeholder Dashboard/Home
tab with a role-dispatched dashboard frame that shows the first real card ("Total units owned" /
unit counts). Every later dashboard story plugs into this.

## Scope
- New `backend/reporting-service` (Clean Architecture, feature-first, JDBC, Flyway, ArchUnit test) with
  its own `reporting_db` (§9) — subject to Q-BOS008-01.
- Consumer framework: envelope parsing per `contracts/kafka/event-envelope.schema.json` (§14.1),
  `processed_event` dedupe table, bounded retry, DLQ (§96).
- building-service extension: write `unit.created`, `unit.updated`, `ownership.assigned` to the existing
  outbox in the same transaction as the change (§8.2, §15); add their `contracts/kafka` schemas.
- Projection `unit_occupancy_view` seed fields from building events (unit id, building, owner(s),
  ownership share); occupancy filled later by rental events (STORY-002/003).
- Access check for reporting reads against building-service membership authority (Q-BOS008-04).
- Gateway route `/api/v1/reports/** -> reporting-service` (§105).
- `GET /api/v1/reports/buildings/{buildingId}/dashboard` returning a role-shaped payload (cards keyed by
  BRD card name, plus `updated_at`).
- Flutter: new `user_app/lib/features/reporting` (data/domain/presentation); Dashboard/Home tab chooses
  §43/§44/§45/§46/§47 layout from the existing `navRoleFor` precedence (BOS-011 Q-01) — subject to
  Q-BOS008-06.

## Acceptance Criteria
1. reporting-service owns `reporting_db`; it never reads another service's database (§9, docs/ARCHITECTURE.md rule 6).
2. Each consumed event is processed at most once per `eventId`; replaying the same event leaves
   projections unchanged (§14.1 rule 3, §16).
3. building-service writes `unit.created`, `unit.updated`, `ownership.assigned` in the same DB
   transaction as the business change and never publishes Kafka directly from the use case (§15, §144).
4. An event that fails after bounded retries lands in a DLQ with the failure reason recorded (§96).
5. Every projection row stores `building_id`; a dashboard request for a building where the caller has no
   active membership returns 403 and reveals no data (§110, §5).
6. `ownership.transferred` updates the unit's owner in the projection: old owner no longer sees the unit,
   new owner does from the effective date (§127, §143 Scenario E).
7. The Dashboard/Home tab no longer shows the starter placeholder; it renders the role layout and the
   owner "Total units owned" card (§44) from the API, with `updated_at` shown.
8. All new UI strings use localization keys (en/bn) (§109).
9. Consumer lag and DLQ size are exposed as metrics; structured logs carry `correlationId` (§95).

## Tasks
Proposed lanes (no TASK files yet):
- BE/DB: reporting-service skeleton, Flyway, consumer framework, dedupe, DLQ, dashboard query use case — L
- BE: building-service outbox events `unit.created`/`unit.updated`/`ownership.assigned` + contracts — M
- BE: api-gateway route + JWT validation for `/api/v1/reports/**` — S
- Mobile: `reporting` feature skeleton, role-dispatched dashboard frame, first card — M
- QA: idempotency/replay test, tenant-isolation test, contract tests for new events (§98) — M
Size: **L** (uncertainty: high — new deployable and infra wiring in compose).

## Dependencies
- BOS-010 building-service outbox (delivered for `ownership.transferred`).
- Q-BOS008-01 (new service approval), Q-BOS008-02 (topic convention), Q-BOS008-04 (access check), Q-BOS008-06 (role → dashboard).

## Out of Scope
Rental/finance/work/communication cards; reports; export; performance load test (STORY-009).

## References
BRD §8.2, §8.9, §9, §14–16, §95–96, §98, §105, §106, §109–110, §127, §143E; `contracts/kafka/`;
`backend/building-service/.../outbox/`; `user_app/lib/app/shell/nav_sets.dart`; UI-INDEX mobile 05–09.
