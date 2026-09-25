# F6-T5a — System-health aggregation read API (BOC-08, slice of F6-T5)

## Category
BE

## Objective
`back-office-service` reports the readiness of every BuildingOS backend service in one call for
the System > System Health screen, by aggregating each service's existing
`/actuator/health/readiness`. No new health-check logic.

## Scope
- Use case `getsystemhealth` (one package) + outbound port `ServiceReadinessProbe`;
  infrastructure client calling `<service-url>/actuator/health/readiness` with a short timeout.
- Services: auth-service, building-service, subscription-service (existing
  `AUTH_SERVICE_URL`, `BUILDING_SERVICE_URL`; add `SUBSCRIPTION_SERVICE_URL` to config and
  local/dev compose), plus back-office-service itself (its own readiness).
- REST: `GET /api/v1/platform/backoffice/system-health` →
  `{ checkedAt, overall: UP|DEGRADED|DOWN, services: [{ name, status: UP|DOWN|UNKNOWN, httpStatus?, latencyMs }] }`
  (`overall` = UP if all UP, DOWN if all non-UP, else DEGRADED). Unreachable/timeout → `UNKNOWN`
  for that service; the endpoint itself still answers 200.
- Service list is configuration (`buildingos.health.services`), not hardcoded business logic.
- OpenAPI contract entry.

## Rules
- Caller must hold `SUPER_ADMIN` (System section, BRD §4.1 / `nav.ts`); otherwise 403.
- Readiness bodies are not forwarded (no component detail leak); only status + HTTP code + latency.

## Acceptance Criteria
- All stubs UP → overall UP; one DOWN (503) → DEGRADED with that service DOWN; one timing out →
  UNKNOWN; non-SUPER_ADMIN → 403; unauthenticated → 401.

## Test Requirements
Unit test for the aggregation rule; integration test with stubbed downstream readiness;
`mvn verify` green for back-office-service (ArchUnit included).

## UI Reference
none (API; screen in F6-T9 ← `ui/stitch/back-office/04`).

## Out of Scope
Audit Logs aggregation (F6-T5b), gateway health, UI (F6-T9).

## References
../TECH-SPEC.md (BOC-08); REQUIREMENTS.md BOC-08; BRD §149.18, §149.21.

## Status
DONE. mvn verify back-office-service green (70 tests, 9 new, ArchUnit 7/7); contract check passed; local compose config valid. Readiness probed without token (platform-web permits /actuator/health/**); bodies never read; any non-2xx = DOWN, timeout/unreachable = UNKNOWN; self probed via own port. dev compose config not checked locally (no infra/dev/.env).
