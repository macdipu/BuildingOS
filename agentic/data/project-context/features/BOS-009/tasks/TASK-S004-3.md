# TASK-S004-3: Local/dev observability stack and dashboards

Status: APPROVED 2026-10-22 (technical approval on RUN-3B8D3AA4). Story: [STORY-004](../stories/STORY-004.md); spec: [STORY-004-TECH-SPEC](../STORY-004-TECH-SPEC.md).

## Category
Security/DevOps

## Objective
Local/dev observability stack and dashboards.

## Scope
infra/local and infra/dev compose: otel-collector, prometheus, grafana, loki, tempo on 127.0.0.1; collector and Prometheus config; Grafana provisioning (datasources + RED and outbox dashboards); service env `OTEL_EXPORTER_OTLP_ENDPOINT`; docs/LOCAL_DEVELOPMENT.md section.

## Dependencies
Q-BOS-009-05 answer

## Implementation Requirements
Follow the tech spec; Boot-managed dependency versions only; match platform-web idiom; no API or business-behavior change.

## Acceptance Criteria
1. `docker compose config` valid for local and dev; the stack starts and Grafana loads both dashboards (AC 6).
2. A request's trace is findable in Tempo and its logs in Loki by correlationId.

## Test Requirements
Compose config validation; manual start on the local stack with screenshots or command output as evidence.

## UI Reference
none

## References
BRD §14.1, §93, §94, §95, §103; STORY-004.

## Out of Scope
Production alerting/topology (STORY-007); consumer lag/DLQ panels (STORY-005).
