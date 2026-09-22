# TASK-003 — Implement independently secured service foundations and gateway routes

## Category
BE

## Status
DONE. Prior session had already built the metadata usecase/controller/security scaffold (independent JwtDecoder per service, audience/issuer/expiry/signature validation, 401/403/503 JSON envelopes, correlation filter) in identity-service, building-service, api-gateway. This session added the missing test evidence: fixture-JWT security matrix (valid/missing/expired/wrong-audience/wrong-issuer/bad-signature/spoofed-header) for all three services via a local JWKS test server + Testcontainers Postgres (identity-service, building-service); gateway routing tests (forward-to-identity, forward-to-building, correlation-id propagation, sanitized 503 on downstream-unavailable); DB-down readiness tests (readiness 503, liveness stays 200) for identity-service/building-service; contracts/openapi/platform.yaml. `mvn -B -f backend/pom.xml verify` — BUILD SUCCESS, all suites green.

## Objective
Implement independently secured service foundations and gateway routes.

## Scope
BOS-001 platform foundation; requirements PF-04,PF-05,PF-06,PF-07.

## Dependencies
TASK-001; TASK-002 for database integration.

## Implementation Requirements
Create service metadata use cases and thin controllers; configure gateway exact-route forwarding. Implement JWT signature/issuer/audience/expiry checks independently at gateway and services. Use generated test keys only in tests. Implement standard envelopes, correlation, health/readiness and sanitized downstream errors.

## Acceptance Criteria
Valid fixture JWT accesses service metadata. Missing, expired, wrong audience/issuer and bad-signature tokens fail at both entrypoints. Service failure returns a sanitized 503; no building header grants access.

## Test Requirements
Security unit/integration matrix; envelope/OpenAPI contract checks; correlation propagation; readiness with database unavailable.

## References
../SRS.md; ../TECH-SPEC.md; ../ARCHITECTURE.md; ../../../BuildingOS_BRD_Agentic_Development.md

## Out of Scope
Real OTP, user data, building membership grants, financial mutations.
