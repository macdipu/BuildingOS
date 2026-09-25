# ADR-001 — Service and local platform boundaries
Status: Proposed; not approved.
Requirements: PF-01–PF-07. Source: BRD §§6–18,93,100,110,141,144.

## Context
BuildingOS mandates independent service data ownership, Kafka, explicit use cases and server-side authorization. A Flutter starter exists; no backend implementation exists.

## Decision proposed
Keep user_app in place. Add gateway, identity and building deployables first. Use separate databases/roles for identity and building on the local PostgreSQL instance. Use Kafka for future domain propagation and require transactional outbox with the first event-producing mutation. Validate JWTs at gateway and services. Keep foundation metadata separate from domain APIs.

## Alternatives
Shared domain database violates explicit BRD constraints.
All-service scaffolding delays a testable first slice.
Production identity-provider choice now would invent missing requirements.
Relocating the existing Flutter project adds unrelated migration risk.

## Consequences
A small runnable foundation precedes user-facing features. Real auth/onboarding, financial allocation and projections remain scoped later tasks with their own tests and required ADRs.
Credentials, CORS, deployment and tenancy checks cannot be inherited blindly from local test fixtures.
No distributed transaction coordinator or cross-service database join is introduced.

## Approval
Pending explicit review of ARCHITECTURE.md and TECH-SPEC.md; this file is not evidence of approval.
