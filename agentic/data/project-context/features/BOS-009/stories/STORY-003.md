# STORY-003: Gateway edge hardening - rate limiting, CORS, circuit breaking

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Implement the §7.1 gateway responsibilities still missing (rate limiting, CORS, circuit
breaking, request logging) and §93 rate limiting, without moving domain logic into the gateway.

## Scope
- api-gateway: request rate limiting keyed by client IP for anonymous routes (OTP start/verify,
  refresh) and by user id for authenticated routes; limits configurable per route group.
- Backing store per Q-BOS-009-04 (recommended Redis, §104 "Rate limit").
- CORS allow-list per environment (back-office web origin only; mobile is not browser-based).
- Circuit breaker + timeouts per downstream route; open circuit returns a normalized
  `ApiEnvelope` error (ARCHITECTURE.md error handling), never a downstream body.
- Request log line (method, route, status, latency, correlationId; no body, no token).
- Infra: Redis service in local/dev compose if approved.

## Acceptance Criteria
1. Exceeding a configured limit returns HTTP 429 with a stable error code and `Retry-After`;
   requests under the limit are unaffected (§93, §7.1).
2. With N gateway instances the limit is shared (only if Redis approved; otherwise the
   per-instance behaviour is documented as a known limit) (§104, §142 "independently scalable").
3. A browser request from an origin not on the allow-list is rejected by CORS; the back-office
   origin is accepted (§7.1).
4. When a downstream service is down, the gateway responds within the configured timeout with
   a normalized error and the circuit opens; other routes keep working (§142 Availability
   "degrade gracefully").
5. Request log contains correlationId and no Authorization header, OTP code or body (§93,
   §94, §95).
6. Gateway still contains no domain/business logic (§7.1); ArchUnit/structure check passes.
7. Tests: integration tests for 429, CORS, circuit-open paths (§97 controller/security tests).

## Tasks
Proposed task lanes only:
- BE: rate limiter + key resolvers + config - M
- BE: CORS + circuit breaker + timeouts + request log - S
- Infra: Redis in compose (if approved) - S
- QA: integration tests - S

Size: M (uncertainty: low-medium; depends on Redis decision).

## Dependencies
- Q-BOS-009-04 (store), Q-BOS-009-02 (limit values). None hard on other stories.

## Out of Scope
- WAF/DDoS protection at the cloud edge (topology, STORY-007). Per-building quotas tied to
  subscriptions (subscription entitlements are not access control, §149.22).

## References
BRD §7.1, §93, §94, §95, §104, §128, §130, §142; agentic/data/project-context/ARCHITECTURE.md.
