# TECH-SPEC-BOS-001 — First runnable platform
## Status
Prepared for technical approval. No backend implementation or runtime pass is claimed.

## Requirement-to-component mapping
PF-01/PF-10 → user_app baseline, backend Maven modules.
PF-02 → infra/docker PostgreSQL bootstrap, per-service Flyway migration and integration tests.
PF-03 → Kafka Compose service and contracts/kafka event-envelope schema.
PF-04/PF-05/PF-06 → gateway route/security configuration, service security filters, OpenAPI.
PF-07 → actuator, structured logs and correlation tests.
PF-08 → local runbook, verification script and CI.
PF-09 → BACKLOG.md.

## Planned files and components
- backend/pom.xml: aggregator with pinned Boot parent/Cloud BOM and Java release.
- backend/api-gateway/: application, route config, security/correlation components, integration tests, Dockerfile.
- backend/identity-service/: application, platform metadata use case/controller, security config, migration, tests, Dockerfile.
- backend/building-service/: same foundation structure under com.buildingos.building; no shared persistence implementation.
- infra/docker/compose.yaml: PostgreSQL, Kafka, gateway and the two services; health checks and named volumes.
- infra/docker/postgres/: database/role bootstrap with per-service privileges.
- contracts/openapi/platform.yaml: metadata and error schemas, bearer security, gateway routes.
- contracts/kafka/event-envelope.schema.json: versioned transport envelope; no invented business payload.
- scripts/verify-platform.sh: deterministic verification with nonzero exit on missing prerequisites/failing tests.
- docs/LOCAL_DEVELOPMENT.md: environment, build/start/verify/shutdown and troubleshooting.
- .github/workflows/platform.yml: backend tests/contract checks/container build; scoped Flutter baseline job.
- user_app: no business behavior modification in this milestone.

## Backend structure
Each service has its own bootstrap application.
platform/domain: immutable metadata value.
platform/application/port/in + usecase: metadata query use case.
platform/presentation/rest: thin controller and DTO mapping.
infrastructure/security + config: JWT validation and error mapping.
No business logic in gateway and no domain dependency on Spring/JPA.
Migration creates only service-local operational schema/metadata needed by foundation; do not precreate incomplete business tables.

## Contract
Metadata query is read-only and requires a validated JWT; it does not reveal principal or building information.
Gateway maps the two exact external platform routes to their exact internal targets.
Return the same JSON envelope and HTTP semantics for proxied and direct authentication failures.
Validate audience explicitly rather than assuming issuer checks imply it.
No business mutation means no idempotency ledger/outbox publisher is implemented yet; the first business writer must implement them together.

## Database isolation
Create identity_db owned by identity_app and building_db owned by building_app.
Revoke default PUBLIC database CONNECT and schema privileges where applicable; grant only each owner's access.
Service credentials are supplied at runtime; examples contain placeholders/local-only setup instructions.
Integration tests attempt cross-database connections with both app roles and assert denial.
Use service-local Flyway history; migration rerun/validation must pass.

## Failure handling
Missing security/database configuration prevents ready state.
Unavailable downstream service returns 503 with SERVICE_UNAVAILABLE and traceId, without exposing internal URLs.
Database outage fails readiness, not process liveness. Broker failure is visible in infrastructure checks.
Do not log Authorization headers, JWT content, personal documents or raw credential values.
Test invalid/missing correlation input and preserve safe trace identifiers.

## Verification
1. Maven reactor verify: compile, unit tests and integration suite.
2. Arch checks: no cross-service JPA/domain imports; gateway does not depend on service persistence modules.
3. Security: missing/expired/bad signature/wrong issuer/wrong audience tokens rejected at gateway AND service; valid test token succeeds for metadata.
4. PostgreSQL: fresh migration, rerun, role isolation and restart persistence.
5. Kafka: broker readiness and a uniquely named non-business smoke topic round trip.
6. HTTP: 200 envelope, 401 envelope and sanitized downstream 503.
7. Compose: config validation; isolated startup; health checks; ordinary shutdown preserves volumes.
8. Flutter: fvm flutter analyze --no-pub and fvm flutter test --no-pub from user_app; record pre-existing failures separately.
9. No production publish/deploy or real financial record creation.

## Execution preparation
Current harness config lacks application build/test commands. Before coding/check execution, reconcile the current preparation run and register reviewed exact commands using the documented lifecycle; never silently alter active pins or bypass the gateway.
Proposed rules: Maven reactor verification from repo root; docker compose -f infra/docker/compose.yaml config; scoped verification script; scoped Flutter analysis/tests.
Container startup requires an effect-aware local execution adapter; a read-only check permission must not disguise service lifecycle effects.
Environment/network approvals may be needed for dependency downloads. Installation of extra software is not assumed.

## Compatibility and rollback
Preserve user_app package name, dependency lock, current env files, template generator, platform signing configuration and existing staged BRD.
Rollback new local deployables by stopping their Compose project; retain volumes and any backups.
No production migration or release approval is included in this scope.

## Open decisions
Q-01–Q-05 stay deferred as in SRS. Container patch/digest selection is an implementation verification detail, not authorization to use unpinned images.
Scope-bound technical approval is required before IMPLEMENTATION.
