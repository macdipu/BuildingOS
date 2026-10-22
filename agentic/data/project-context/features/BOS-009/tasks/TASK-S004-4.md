# TASK-S004-4: Mobile correlation header and end-to-end observability check

Status: APPROVED 2026-10-22 (technical approval on RUN-3B8D3AA4). Story: [STORY-004](../stories/STORY-004.md); spec: [STORY-004-TECH-SPEC](../STORY-004-TECH-SPEC.md).

## Category
QA

## Objective
Mobile correlation header and end-to-end observability check.

## Scope
user_app ApiClient: per-request `X-Correlation-Id` (add if missing, no PII in client logs); end-to-end check on the local stack.

## Dependencies
TASK-S004-1, -2, -3

## Implementation Requirements
Follow the tech spec; Boot-managed dependency versions only; match platform-web idiom; no API or business-behavior change.

## Acceptance Criteria
1. Every app request carries a UUID `X-Correlation-Id`.
2. End to end: app or curl request → JSON logs with the same correlationId in each service → one trace in Tempo.

## Test Requirements
Flutter unit test for the header; recorded local-stack evidence.

## UI Reference
none

## References
BRD §14.1, §93, §94, §95, §103; STORY-004.

## Out of Scope
Production alerting/topology (STORY-007); consumer lag/DLQ panels (STORY-005).
