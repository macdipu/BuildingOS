# HLD: BuildingOS system architecture

## Status

DRAFT, 2026-09-24. Scoped synthesis of existing architecture and BOS-010 delivery
evidence, not a fresh whole-repository audit or approved replacement for
[docs/ARCHITECTURE.md](../../../docs/ARCHITECTURE.md).

## Purpose and Scope

Multi-building platform with global user identity, reviewed building applications,
building administration and per-user subscriptions. This document distinguishes the
shipped foundation/F2/F5a boundaries from proposed F4 and future domains.

## System Context

Mobile users and platform operators authenticate with auth-service and access business
APIs through the gateway. Backend services validate JWTs independently. Local Postgres,
Kafka and MinIO support development; production environment topology is not certified
by this draft. Back-office web, rental, finance and reporting remain future slices.

## Architecture Drivers

### Key Requirements

Global User identity; separate platform/building roles and ownership; audited building
review/activation; per-user entitlements; tenant isolation; explicit state transitions.
F4 proposes unit/ownership flows and the valid-unit activation prerequisite.

### Quality Attributes / NFRs

Preserve authorization and transaction correctness, history, traceability and replaceable
integration boundaries. No numeric availability/throughput commitments are invented.

### Constraints

Microservices in one monorepo, Clean Architecture, feature-first, one use case per
action, repository interfaces inward and JDBC adapters outward. No cross-service
database reads or weakened architecture checks.

## Architectural Style and Key Patterns

Presentation → application → domain; infrastructure implements ports. Commands own local
transactions. HTTP contracts live under contracts/openapi; Kafka contracts under
contracts/kafka. Flutter features have data/domain/presentation layers and use shared core
interfaces without importing another feature's internals.

## Component / Service Map

| Component | Shipped responsibility | Proposed / later work |
|---|---|---|
| auth-service | Phone OTP, global user, platform roles, JWT/JWKS, initial-admin user provisioning | Live SMS adapter/production hardening separately |
| building-service | Applications/review/documents, buildings/lifecycle, initial memberships | F4 units/ownership/invitations; single membership authority decision proposed |
| subscription-service | Plan catalog, user subscriptions, free tier, creation-fee records/status | Domain-specific enforcement and paid feature UX |
| api-gateway | Explicit external routes, independent token validation, correlation | F4 public routes |
| platform-web | Shared response/error/security/correlation kernel | Shared transport conventions only |
| user_app | Authentication and application submission | F4 portfolio/unit/ownership flows |
| back-office web / rental / finance / reporting | Not asserted implemented by this scope | Separate feature delivery |

## Data Architecture

Each service owns its database and credentials. building-service owns its current
Building/Membership rows; auth-service owns the global User. Subscription state is
per-user per BOS-010 D-22, replacing the BRD building-subscription model.
Document bytes live in S3-compatible storage; metadata/authorization belongs to the
owning business feature. No secret/token/document bytes in ordinary logs.

## Integration and Contracts

F2 approval checks the creation fee and provisions its chosen admin through HTTP
before locally creating the building/membership/transitions. F2 lifecycle events are
deferred by D-13. F4 proposes a transactional ownership outbox and versioned event;
it is not yet implemented. Stable IDs and explicit ownership replace shared DB access.

## Deployment and Runtime Topology

Local Docker Compose supplies Postgres, Kafka and MinIO; services are separate Java
applications. Startup and readiness guidance lives in docs/LOCAL_DEVELOPMENT.md.
The Flutter app calls the gateway. Do not infer production deployment from local/test
release approval.

## Cross-Cutting Concerns

### Security and Identity

JWT issuer/audience/signature are validated at gateway/services. Current F2 business
authorization uses platform roles and application ownership. F4 database membership/
ownership checks are proposed, not present behavior.

### Observability

Correlation IDs, readiness endpoints and business audit records are established
patterns. F4 outbox retry/age/conflict metrics are proposed additions.

### Error Handling and Resilience

Use ApiEnvelope/ApiError, explicit domain failures and fail-closed dependency handling.
Preserve transaction boundaries and avoid leaking SQL/credentials/downstream bodies.

### Configuration and Secrets

Environment/secret-managed credentials; replaceable service/storage URLs. Development
OTP is local/test only. Production auth, scanning, backup and operational readiness
remain their own explicit gates.

## Technology Stack

Existing repository: Java 17/Spring, Maven, JDBC/Postgres/Flyway, Kafka, S3-compatible
storage, Flutter/GetX. This draft selects no new library versions or infrastructure.

## Architecture Decisions

- Canonical architecture standard: docs/ARCHITECTURE.md, previously operator-approved.
- BOS-010 D-22..D-27: per-user revenue model and creation-fee approval requirement.
- [F4 membership ADR](features/BOS-010/units-ownership/adr/ADR-F4-001-membership-authority.md):
  PROPOSED; retain the shipped building-service membership authority as an explicit
  deviation from BRD §8.1.

## Feature Architecture Index

- [BOS-001 foundation HLD](features/BOS-001/ARCHITECTURE.md): historical/superseded
  planning context; current architectural rules live in docs/ARCHITECTURE.md.
- [BOS-010 F4 units/ownership HLD](features/BOS-010/units-ownership/ARCHITECTURE.md):
  PROPOSED, awaiting requirements/technical approval.
- F2 and F5a existing technical context: [F2](features/BOS-010/building-application/TECH-SPEC.md),
  [F5a](features/BOS-010/subscription-plans/TECH-SPEC.md). These predate separate HLD files.

## Risks and Technical Debt

BRD membership placement and current F2 placement differ; F4 proposes resolving this
explicitly. Legacy context summaries may lag delivered slices; use their dated scoped
release/baseline records. No general production-readiness claim.

## Open Questions

F4 product choices, membership ADR and technical acceptance; later back-office technology
and scoped support/onboarding grants. See the F4 decision packet for the current gate.

## References

[Architecture standard](../../../docs/ARCHITECTURE.md),
[BRD](BuildingOS_BRD_Agentic_Development.md),
[BOS-010 tasks](features/BOS-010/TASKS.md).
