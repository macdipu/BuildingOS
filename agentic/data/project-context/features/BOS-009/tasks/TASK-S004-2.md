# TASK-S004-2: Distributed tracing with OpenTelemetry and trace context through the building-service outbox; outbox metrics

Status: APPROVED 2026-10-22 (technical approval on RUN-3B8D3AA4). Story: [STORY-004](../stories/STORY-004.md); spec: [STORY-004-TECH-SPEC](../STORY-004-TECH-SPEC.md).

## Category
BE

## Objective
Distributed tracing with OpenTelemetry and trace context through the building-service outbox; outbox metrics.

## Scope
Micrometer Tracing OTel bridge + OTLP exporter (Boot-managed); propagation on inbound HTTP, RestClient clients, api-gateway; Flyway migration `building_outbox.trace_parent`; outbox writer/publisher; gauges `outbox_pending_events`, `outbox_oldest_pending_age_seconds`.

## Dependencies
TASK-S004-1

## Implementation Requirements
Follow the tech spec; Boot-managed dependency versions only; match platform-web idiom; no API or business-behavior change.

## Acceptance Criteria
1. A request through the gateway to back-office and its downstream calls share one trace id (AC 1).
2. An outbox-published event carries `traceparent` and `correlationId` Kafka headers from the originating request (AC 2).
3. `/actuator/prometheus` on building-service exposes the two outbox gauges (AC 4); actuator exposure otherwise unchanged (AC 5).
4. With no `OTEL_EXPORTER_OTLP_ENDPOINT`, services start and tests pass without a collector.

## Test Requirements
Integration test: outbox row stores trace_parent and the published record carries the header (existing Kafka Testcontainer); gauge test; full `mvn verify`.

## UI Reference
none

## References
BRD §14.1, §93, §94, §95, §103; STORY-004.

## Out of Scope
Production alerting/topology (STORY-007); consumer lag/DLQ panels (STORY-005).
