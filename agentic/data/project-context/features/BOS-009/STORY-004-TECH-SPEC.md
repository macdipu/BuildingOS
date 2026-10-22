# BOS-009 STORY-004 — observability baseline: technical spec (LLD)

Status: APPROVED 2026-10-22 by operator (AskUserQuestion: "Approve spec (Recommended)"). Run `RUN-3B8D3AA4AF8545DE82824F42175FA4C1`.
Story: [STORY-004](stories/STORY-004.md). Open question: Q-BOS-009-05 (stack).

## Baseline (verified 2026-10-22)

- Spring Boot 4.1.1 parent; `platform-web` already ships `micrometer-registry-prometheus`; actuator exposes
  `health,info,prometheus` with liveness/readiness groups.
- `CorrelationFilter` (platform-web) accepts/creates `X-Correlation-Id`, echoes it, puts MDC `traceId`, logs
  one plain-text `http_request` line. No JSON logs, no distributed tracing, no redaction.
- building-service is the only event producer: `building_outbox` already stores `correlation_id`;
  `OutboxPublisherJob` publishes later, outside the request.
- No observability containers in `infra/local` or `infra/dev`.

## Design

1. **Structured logs (all services, via platform-web).** Use Spring Boot's built-in structured logging
   (`logging.structured.format.console: logstash`), set in each service's `application.yaml` (plain text
   kept for the `test` profile). JSON members: `@timestamp`, `level`, `logger`, `message`, `service`,
   `traceId`, `spanId`, `correlationId`. MDC key for the request id is renamed to `correlationId`; `traceId`
   becomes the OpenTelemetry trace id. The `X-Correlation-Id` header and `ApiError.traceId` contract stay
   unchanged (no API change).
2. **Tracing.** Micrometer Tracing with the OpenTelemetry bridge and the OTLP exporter (Boot-managed versions,
   no new version pins). W3C `traceparent` propagation on inbound HTTP, `RestClient` (back-office and
   building-service clients) and the gateway. Sampling: 100% local/dev (`TRACING_SAMPLING_PROBABILITY`, default
   1.0); production value deferred to STORY-007. Export endpoint `OTEL_EXPORTER_OTLP_ENDPOINT`; tracing
   export disabled when unset (tests and plain `mvn verify` need no collector).
3. **Kafka trace context through the outbox.** A Flyway migration adds nullable `trace_parent varchar(55)` to
   `building_outbox`; the outbox writer stores the current `traceparent`; `OutboxPublisherJob` sets it as the
   Kafka `traceparent` header (with `correlationId`, already present) so a future consumer span links to the
   originating request (AC 2). Rows written before the migration publish without the header.
4. **Redaction.** A platform-web log-message customizer masks, in message and structured members:
   BD phone `01XXXXXXXXX` → `01*******NN`; JWT-shaped tokens (`eyJ…\.…\.…`) → `[jwt]`; values of keys
   `otp|code|password|secret|token|refreshToken|nid|nationalId` → `[redacted]`; presigned-URL query strings
   (`X-Amz-Signature`) dropped. Unit tests cover each pattern (AC 3).
5. **Metrics.** Keep Prometheus. Add building-service gauges `outbox_pending_events` and
   `outbox_oldest_pending_age_seconds` (from `building_outbox`), and enable HTTP client/server, JVM and Hikari
   metrics (Boot defaults). Actuator exposure unchanged; only liveness/readiness are public, other endpoints
   keep their current authorization (AC 5).
6. **Local/dev stack (Q-BOS-009-05 option a).** Compose services bound to `127.0.0.1`: OpenTelemetry
   Collector (OTLP in, traces → Tempo, logs → Loki), Prometheus (scrapes each service's
   `/actuator/prometheus`), Grafana (provisioned datasources and two dashboards: per-service RED metrics,
   building-service outbox), Loki, Tempo. `infra/local` uses `:latest` like the existing file; `infra/dev`
   pins versions. Services get `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318`. Docs in
   `docs/LOCAL_DEVELOPMENT.md` (ports, how to find a trace by correlation id).
7. **Mobile.** Verify `user_app` `ApiClient` sends a per-request `X-Correlation-Id`; add it if missing. No PII
   in client logs.

## Tasks

| Task | Category | Scope | Size |
|---|---|---|---|
| [TASK-S004-1](tasks/TASK-S004-1.md) | BE | platform-web structured logging, MDC, redaction + tests; per-service yaml | M |
| [TASK-S004-2](tasks/TASK-S004-2.md) | BE/DB | tracing bridge + OTLP, RestClient/gateway propagation, outbox `trace_parent` migration + header, outbox gauges | M |
| [TASK-S004-3](tasks/TASK-S004-3.md) | Security/DevOps | compose observability stack (local + dev), Grafana provisioning, docs | M |
| [TASK-S004-4](tasks/TASK-S004-4.md) | Mobile + QA | correlation header check; trace-propagation integration test; end-to-end check on the local stack | S |

Order: 1 → 2; 3 in parallel; 4 last.

## Risks

- Log format change: anyone grepping plain-text logs must switch to JSON (doc note).
- `traceId` meaning changes from the correlation id to the OTel trace id in logs; correlation stays as
  `correlationId` and in `ApiError.traceId`. Recorded here so support docs are updated.
- Boot 4 OpenTelemetry starter coordinates must be confirmed against the managed BOM at implementation.

## Approval

Technical approval: APPROVED 2026-10-22, "Approve spec (Recommended)". Q-BOS-009-05: ANSWERED option (a) self-hosted OSS, "Self-hosted OSS (Recommended)"; production hosting follows Q-BOS-009-08.
