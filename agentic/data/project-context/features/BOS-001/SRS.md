# SRS-BOS-001 — Platform foundation
Status: Approved BOS-001 scope implemented; release approval pending. Source: ../../BuildingOS_BRD_Agentic_Development.md.
This milestone enables development; it does not claim the product's MVP acceptance scenarios are implemented.

## Actors and flows
Developer: checkout → configure local values → build → start infrastructure/services → inspect health → run integration tests.
Authenticated test principal: gateway → token validation → identity/building foundation endpoint → service token validation → standard response.
Unauthenticated/invalid principal: gateway or direct service request → 401 without private data.
Operator: inspect structured logs/metrics → identify failed dependency; shut down without deleting persistent data.

## Requirements and acceptance
| ID | Requirement | BRD | Observable acceptance |
|---|---|---|---|
| PF-01 | Preserve existing Flutter app and add separate gateway, identity and building deployables | 6,10,36,100,123 | Independent module build; no cross-domain entities/JPA imports; user_app stays at its current path |
| PF-02 | PostgreSQL database and credentials owned per service | 9,99,110 | identity role cannot connect to building DB and vice versa; Flyway migration runs once and reruns cleanly |
| PF-03 | Kafka local infrastructure and versioned event contract convention | 14–17,96 | Broker health check and produce/consume smoke pass; envelope schema includes unique eventId, eventVersion, occurredAt, producer, correlationId and buildingId when applicable |
| PF-04 | Versioned API conventions | 18,98,118,128 | Success and error envelopes validated; missing/invalid token returns 401; no raw stack traces |
| PF-05 | Independent authentication enforcement | 5,7,93,110 | Gateway and direct services reject unsigned/expired/wrong-issuer/wrong-audience JWTs; building header alone grants no access |
| PF-06 | Fail-closed configuration | 93,103 | No embedded production key/password; non-local runtime refuses missing issuer/audience/database config; development issuer cannot activate accidentally |
| PF-07 | Observability | 95,101,134 | Liveness/readiness, restricted metrics, structured logs with trace/correlation identity; no token/NID logging |
| PF-08 | Reproducible local setup and checks | 97,101–103,120–122 | Maven verify, Compose config/health, migration/isolation and security integration tests; Flutter baseline captured when execution is configured |
| PF-09 | Dependency-aware roadmap | 123–125,143,146 | Every MVP success item maps to a delivery phase; financial rules are explicit test acceptance |
| PF-10 | Preserve client integration seams | 31–42,109,147 | Record API/auth mismatch; keep GetX/Clean Architecture/theme/localization; no fake dashboard, bypass login or insecure token storage introduced |

## Foundation boundaries
No building membership creation, arbitrary role grants, financial API, real OTP, or production deployment.
Foundation endpoints return service metadata only, never real building or personal data.
No business events are published until corresponding domain operations and transactional outbox are implemented.
Security test JWTs are generated in test fixtures and never become a production credential or login mechanism.

## Future invariant acceptance
FIN-01: 25,000 due and 15,000 payment yields 10,000 remaining through allocation (§143B).
FIN-02: 50,000 payment against 20,000 invoice creates a 30,000 advance ledger entry (§143C).
FIN-03: Offline replay uses the same idempotency key and returns one official payment/receipt (§143D).
FIN-04: Payment reversal retains original record, reverses allocations and emits audit (§§24,113).
OWN-01: Transfer closes the old period, preserves history and leaves the lease unchanged (§143E).
AUTH-01: Owner with three units sees those units only (§143A).
These are backlog criteria, not tests executed in BOS-001.

## Unresolved questions
| ID | Decision | Needed before | Effect on BOS-001 |
|---|---|---|---|
| Q-01 | SMS/OTP and Google provider, expiry/retry/rate limits | Real login | Nonblocking: protected foundation routes only |
| Q-02 | Initial platform administrator and building membership grant process | Building onboarding | Nonblocking: no grants or onboarding in foundation |
| Q-03 | Resolve §17 invoice-owner contradiction | Rental/lease activation | Nonblocking: no rental/finance writes |
| Q-04 | Complete role/object authorization matrix, maintenance calculation policy | Respective feature | Nonblocking: foundation exposes no domain data |
| Q-05 | Production hosting, retention, backups and secrets manager | Deployment | Nonblocking: local development scope only |
