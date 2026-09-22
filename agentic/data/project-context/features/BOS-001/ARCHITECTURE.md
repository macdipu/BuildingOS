# ARCH-BOS-001 — Local platform foundation
## Status
Proposed; awaiting scope-bound technical approval. Implements PF-01 through PF-10.

## Reuse
Keep user_app and its GetX/Clean Architecture layers, themes, route bindings, feature generator and localization.
Existing email-login wire format is not compatible with the BRD; defer replacement to BOS-002.
Backend is new; use feature-first/layer-second packages, not global controllers/services/repositories.

## Components
| Component | Owns | Does not own |
|---|---|---|
| api-gateway | Routing, request correlation, authentication validation, configurable CORS | Business decisions, persistence, membership grants |
| identity-service | Identity/membership boundary and protected service metadata foundation | Building records, real OTP provider in this milestone |
| building-service | Building boundary and protected service metadata foundation | Identity store, ownership changes in this milestone |
| PostgreSQL local server | Separate identity_db/building_db with separate credentials | Shared domain schema or cross-service joins |
| Kafka local KRaft broker | Local event transport health/smoke test | Business events without durable domain operations |
| user_app | Existing mobile/web client | Server authorization or authoritative finance state |

Later services are added when their vertical slices begin, using the service ownership table in BRD §9.

## Data and control flow
Client → gateway → target service.
Gateway and service independently validate issuer, audience, expiry and signature.
Foundation metadata contains no user/building data; future building APIs must additionally enforce membership and permissions against the active building.
Service → its own database using its own connection role.
Future mutations → same-transaction domain state + outbox → Kafka → idempotent consumer → receiving service store.
No synchronous cross-service database queries or distributed two-phase commit.

## Local security boundary
Publish gateway on localhost. Keep database, broker and service ports internal unless an explicit local debug profile binds them to localhost.
Health liveness returns minimal status; readiness reflects dependencies. Detailed actuator info/metrics require authenticated access or a private management network.
Tests use short-lived generated signing keys and a test JWKS/issuer fixture. Production profiles never load these fixtures.
Runtime token validation uses externally supplied issuer/JWKS and expected audience; no fabricated successful login screen.
Use explicit route/CORS allowlists; do not forward unauthenticated membership/role headers as trusted identity.

## API conventions
/api/v1 for future domain APIs. Protected foundation metadata:
GET /api/v1/platform/identity → identity service /internal/platform/info.
GET /api/v1/platform/building → building service /internal/platform/info.
Response: {success:true,data:{service:string},meta:{},traceId:string}.
Error: {success:false,code:string,message:string,traceId:string}, with fields for validation errors.
These routes are operational foundation endpoints, not building/profile APIs.
Use 401 AUTH_REQUIRED, 403 ACCESS_DENIED, 503 SERVICE_UNAVAILABLE as applicable.
Preserve trace identity through gateway and service; reject/sanitize invalid caller correlation headers.

## Dependencies
Propose Java 17 bytecode, Maven, Spring Boot 4.1.1 and Spring Cloud BOM 2025.1.3.
The installed Java 17/Maven 3.9.12 meet documented minimums; dependency resolution is still an execution check.
Official [Boot requirements](https://docs.spring.io/spring-boot/system-requirements.html) and [Cloud compatibility](https://spring.io/projects/spring-cloud/) checked 2026-09-22.
Pin PostgreSQL/Kafka container releases and digests after registry verification during implementation; do not commit floating latest tags.

## Alternatives and consequences
- Moving user_app to apps/: no immediate value; preserve its path and build assumptions.
- Single shared domain DB: conflicts with mandatory ownership in BRD §9; use distinct databases/users even on a shared local server.
- Creating all 12 services now: increases empty scaffolding; deliver three foundational deployables then vertical slices.
- Real identity provider now: requires unspecified business/provider choices; foundation validates tokens without implementing login.
- Custom distributed framework: unnecessary; use Spring conventions and focused service-local infrastructure.

## Migration, rollback and risks
Only new backend/infrastructure/contracts files in this milestone; no production data migration.
Local PostgreSQL volumes survive normal shutdown. Deletion of volumes is never an automatic rollback.
Flyway validates versioned migrations; no edits to applied migrations.
Configuration, service health, Docker daemon and dependency downloads must be verified before claiming runtime readiness.
The approved architecture does not resolve future product questions Q-01–Q-05.

## ADRs
See adr/ADR-001-platform-boundaries.md. Later finance, sync, reporting and real authentication decisions get scoped ADRs when their contracts are ready.
References: FEATURE.md, SRS.md, BASELINE.md; BRD §§6–18,93–110,123,141,144.
