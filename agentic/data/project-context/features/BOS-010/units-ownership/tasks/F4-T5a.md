# F4-T5a — Ownership outbox publisher (Kafka)

## Category
DB/Integration (building-service)

## Objective
Deliver the `building_outbox` events that F4-T4a writes transactionally (UO-10) to Kafka at least
once, with a stable event ID, ordered per unit, surviving broker outages. F4-T5 is split: T5a
publisher, T5b transfer documents (UO-13).

## Scope
- `outbox` feature: `PublishPendingEventsUseCase` locks due rows (`FOR UPDATE SKIP LOCKED`), publishes
  each via the `EventPublisher` port, marks delivered or records attempt/backoff/last error. A failed
  event holds back later events of the same aggregate in that pass.
- `KafkaEventPublisher` (spring-kafka, operator decision 2026-09-24): topic = event type, key =
  aggregate (unit) id, value = `contracts/kafka/event-envelope.schema.json` envelope, producer
  `building-service`, correlationId = request trace id or the event id. Idempotent producer, acks=all.
- Scheduled worker, topics created on startup, backlog gauges and publish counters.
- Config `buildingos.outbox.*` and `spring.kafka.*` (env vars in docs/LOCAL_DEVELOPMENT.md); the
  test profile disables the worker except in the Kafka integration test.

## Acceptance Criteria
- A committed transfer event reaches Kafka in the envelope contract with the outbox event ID.
- Kafka unavailable: the event stays pending with attempts/last error, and is delivered with the
  same ID after recovery. Delivered rows are not re-sent.
- No contact details or document content in payloads (unchanged ownership.transferred v1 data).

## Status
COMPLETED 2026-09-24 (run RUN-C73FA516545143FAA9B35B168EEFE0C9). PublishPendingEventsServiceTest 4,
OutboxKafkaIntegrationTest 2 (apache/kafka compose image, real pause/unpause outage). building-service
verify 106/0, ArchUnit 7/0, platform-web 5/0, check-contracts passed.
