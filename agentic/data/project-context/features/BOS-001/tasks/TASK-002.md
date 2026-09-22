# TASK-002 — Provide isolated local PostgreSQL and Kafka

## Category
DB/Integration

## Status
DONE. infra/docker/compose.yaml (Postgres 17-alpine + apache/kafka 3.9.0, both image-digest pinned, ports bound to 127.0.0.1, named volumes, health checks); infra/docker/postgres/init/ creates identity_db/identity_app and building_db/building_app with PUBLIC connect revoked; contracts/kafka/event-envelope.schema.json added; scripts/verify-platform.sh automates fresh-start + migration rerun + cross-db isolation denial + Kafka smoke round trip + restart-persistence, run via the governed gateway (`sh scripts/verify-platform.sh`) — VERIFY PASSED 2026-09-22.

## Objective
Provide isolated local PostgreSQL and Kafka.

## Scope
BOS-001 platform foundation; requirements PF-02,PF-03,PF-08.

## Dependencies
TASK-001 build/layout conventions.

## Implementation Requirements
Add local Compose topology, database-specific roles/privileges, service migrations, Kafka health checks and versioned envelope schema. Pin verified image releases/digests. Bind published developer ports to localhost; retain named volumes on normal shutdown.

## Acceptance Criteria
Fresh startup and migration rerun pass. Each app credential cannot connect to the other domain database. Broker smoke round trip passes and no business events are fabricated.

## Test Requirements
Compose config; Testcontainers or equivalent isolated migration/isolation tests; broker smoke; restart persistence.

## References
../SRS.md; ../TECH-SPEC.md; ../ARCHITECTURE.md; ../../../BuildingOS_BRD_Agentic_Development.md

## Out of Scope
Business tables, finance/outbox operations without domain implementation, production infrastructure.
