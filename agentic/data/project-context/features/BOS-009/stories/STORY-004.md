# STORY-004: Observability baseline - JSON logs, tracing, metrics, redaction

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Every service meets §95: structured JSON logging, `traceId`, `correlationId`, metrics, health
endpoints, distributed tracing; and §94 "Do not include NID in logs".

## Scope
- platform-web (shared kernel): JSON log encoder, MDC with `traceId`/`spanId`/`correlationId`,
  OpenTelemetry tracing (§95 recommended), propagation over HTTP and Kafka headers (outbox
  publisher in building-service).
- Log redaction filter for phone numbers (masked), NID, tokens, OTP codes, document URLs with
  signatures (§93 signed URL, §94).
- Metrics: existing Prometheus endpoint kept; add HTTP server/client, JVM, DB pool, outbox
  pending/age/failure (ARCHITECTURE.md "F4 outbox retry/age/conflict metrics are proposed").
- Health: liveness/readiness already present; confirm Kafka/MinIO readiness where used.
- Infra: observability stack in local/dev compose per Q-BOS-009-05 (recommended BRD set:
  OTel Collector, Prometheus, Grafana, Loki, Tempo).
- Baseline Grafana dashboards: per-service RED metrics, outbox, OTP (STORY-001).
- Flutter: send `X-Correlation-Id` per request (if not already) so mobile issues can be traced;
  no PII in client logs.

## Acceptance Criteria
1. Every service (auth, building, subscription, back-office, gateway) logs JSON lines carrying
   `traceId` and `correlationId`; a single request through the gateway shows one trace across
   services (§95).
2. An outbox-published event carries trace context in Kafka headers and the consumer span links
   to it (§95 distributed tracing; §14.1 `correlationId`).
3. Automated test: logging an object containing NID, OTP code, JWT, refresh token produces
   masked output (§94 "Do not include NID in logs", §93).
4. `/actuator/prometheus` exposes outbox pending count and oldest-pending age for
   building-service (§95 metrics).
5. Health endpoints remain public only for liveness/readiness; other actuator endpoints are not
   exposed externally (§93 server-side authorization).
6. Local compose brings up the stack and the dashboards load (§103 local).

## Tasks
Proposed task lanes only:
- BE: platform-web logging/tracing/redaction module - M
- BE: per-service adoption + outbox metrics - M
- Infra: observability stack in compose + dashboards - M
- Mobile: correlation header check - S
- QA: redaction tests, trace propagation integration test - S

Size: L (uncertainty: medium; stack choice and retrofit breadth).

## Dependencies
- Q-BOS-009-05 (stack). None hard on other stories.

## Out of Scope
- Production alert routing (STORY-007 topology). Consumer lag/DLQ panels (STORY-005).
- Business analytics dashboards (BOS-008).

## References
BRD §14.1, §93, §94, §95, §101 ("health checks, metrics"), §103; agentic/data/project-context/
ARCHITECTURE.md (Observability); docs/ARCHITECTURE.md.
