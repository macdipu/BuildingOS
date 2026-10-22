# TASK-S004-1: Structured JSON logging, MDC and log redaction in platform-web, adopted by every service

Status: APPROVED 2026-10-22 (technical approval on RUN-3B8D3AA4). Story: [STORY-004](../stories/STORY-004.md); spec: [STORY-004-TECH-SPEC](../STORY-004-TECH-SPEC.md).

## Category
BE

## Objective
Structured JSON logging, MDC and log redaction in platform-web, adopted by every service.

## Scope
platform-web logging config + redaction customizer; `application.yaml` of auth, building, subscription, back-office, gateway (`logging.structured.format.console: logstash`; plain text under the test profile); MDC `correlationId`.

## Dependencies
none

## Implementation Requirements
Follow the tech spec; Boot-managed dependency versions only; match platform-web idiom; no API or business-behavior change.

## Acceptance Criteria
1. Every service logs one JSON object per line with service, level, logger, message, traceId, spanId, correlationId (STORY-004 AC 1).
2. Logging a message or structured member containing a BD phone, a JWT, an OTP/password/secret/token/NID value or a presigned URL emits the masked form (AC 3, BRD §93, §94).
3. `X-Correlation-Id` header and `ApiError.traceId` unchanged.

## Test Requirements
Unit tests per redaction pattern; one test per service that boot logs are valid JSON (or a platform-web test with the shared config).

## UI Reference
none

## References
BRD §14.1, §93, §94, §95, §103; STORY-004.

## Out of Scope
Production alerting/topology (STORY-007); consumer lag/DLQ panels (STORY-005).
