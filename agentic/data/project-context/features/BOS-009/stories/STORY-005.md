# STORY-005: Kafka consumer failure handling, DLQ and visibility

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Provide the §96 consumer policy (retry transient, bounded retry, DLQ with reason, alert for
financial DLQ) and §14.1 rule 3 idempotent consumers as a shared, reusable capability, and make
DLQ and consumer lag visible to platform operators (§95).

Observed 2026-10-22: building-service has an outbox producer; no service has a Kafka consumer
yet. The capability is therefore proven with a test consumer here and adopted by the first real
consumers in BOS-003+ (BACKLOG: "Outbox and consumer idempotency ship with each
event-producing feature").

## Scope
- platform-web: consumer support - error classification (transient vs unrecoverable), bounded
  retry with backoff, DLQ publish with reason headers (exception class, message summary,
  attempt count, source topic/partition/offset, `eventId`), idempotent-consumer store
  (`processed_event` table pattern + Flyway template, §99).
- Envelope validation against `contracts/kafka/event-envelope.schema.json`; invalid envelope ->
  DLQ without retry.
- Topic naming `<topic>.dlq` (§96 example `finance.events.dlq`; Q-BOS-009-06).
- Outbox publisher (building-service): confirm bounded retry and `FAILED` status visibility
  through STORY-004 metrics.
- back-office-service System Health (BOC-08) + `buildingos_backoffice_web`: read-only panel of
  consumer group lag and DLQ message count per topic (§95 "Kafka consumer lag monitoring",
  "Dead-letter queue visibility"; §149.2 System Health). Platform permission required
  (§149.22).
- Alert rule: any message on a financial DLQ (`finance.events.dlq`) raises an operations alert
  (§96 step 5); rule defined now, active once finance-service exists (BOS-004).
- DLQ replay: documented runbook (manual re-publish of a DLQ message after fix), per §135
  "Replay policy"; UI replay is not in scope (Q-BOS-009-06).

## Acceptance Criteria
1. A consumer that throws a transient error retries up to the configured bound with backoff and
   then succeeds without duplicate side effects (§96 steps 1-2).
2. An unrecoverable error (or retries exhausted) publishes the original record to the DLQ with
   the reason headers listed above and commits the source offset (§96 steps 3-4).
3. Delivering the same `eventId` twice applies the side effect once (§14.1 rule 3, §14.1 rule
   4); test uses Testcontainers Kafka + Postgres (§97 Kafka consumer tests).
4. An envelope missing `eventId`, `occurredAt` or `eventVersion` goes to DLQ (§14.1 rules 4,
   5, 7).
5. System Health shows per-topic DLQ count and consumer lag; non-platform callers get 403 and
   no data (§95, §149.22).
6. An alert rule for `finance.events.dlq` exists in the alerting config and is tested with a
   synthetic message (§96 step 5).
7. Replay runbook re-publishes one DLQ message and the idempotent consumer processes it once
   (§135 replay policy).

## Tasks
Proposed task lanes only:
- BE: platform-web consumer retry/DLQ/idempotency module + Flyway template - M
- BE: back-office lag/DLQ read (Kafka admin client) - S
- Web: System Health DLQ/lag panel - S
- Infra: alert rule + DLQ topics in compose - S
- QA: Testcontainers consumer/DLQ/idempotency tests; runbook drill - S

Size: L (uncertainty: medium; no real consumer to validate against yet).

## Dependencies
- Q-BOS-009-06 (retry bounds, DLQ naming, replay UI). Soft: STORY-004 (metrics/alerting stack).
- Real consumers arrive with BOS-003/004/007/008.

## Out of Scope
- Domain consumers themselves. Automated DLQ replay UI. Exactly-once claims across crashes
  (TASK-003-READINESS: do not claim exactly-once).

## References
BRD §14.1, §14.2, §15, §16, §95, §96, §97, §135, §149.2, §149.20, §149.22;
contracts/kafka/event-envelope.schema.json; features/BOS-001/BACKLOG.md;
ui/stitch/gaps/back-office.md (System Health unmocked); D-13.
