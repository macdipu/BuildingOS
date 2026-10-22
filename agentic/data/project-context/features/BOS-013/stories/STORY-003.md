# STORY-003: Internal membership, unit and ownership lookup for services

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M (needs a D-38 amendment).

## Parent Epic
EPIC-BOS-013 ([EPIC.md](../EPIC.md))

## Objective
Give rental, finance, work, communication, notification and reporting services one authoritative,
fail-closed way to ask building-service "what may this user do in this building?", "who holds role
R here?" and "what is unit U and who owns it now?" — following the D-38 service-JWT pattern of
`DirectoryBuildingController` (§106: REST when an immediate answer is needed).

## Scope
- auth-service: register a service client per consuming service (client credentials, secrets from
  env, constant-time compare as in D-38) issuing tokens with the approved scope (Q-BOS-013-07,
  default `membership.read`).
- building-service internal endpoints (not routed by the gateway), proposed shapes:
  - `GET /internal/directory/buildings/{b}/members/{userId}` → active roles, permissions
    (STORY-001 catalogue), titles, building status;
  - `GET /internal/directory/buildings/{b}/members?role=&page=&size=` → paged user ids + roles
    (audience resolution, Q-BOS-007-03);
  - `GET /internal/directory/buildings/{b}/units/{unitId}` → unit fields + current ownership
    periods (Q-BOS-004-31, Q-BOS-003-08);
  - `GET /internal/directory/buildings/{b}/units/{unitId}/owners` or an ownerships-by-user read,
    if Q-BOS-013-07 review keeps it.
- No personal data beyond ids in responses (§94); phone/name stay behind public endpoints.

## Acceptance Criteria
1. Each endpoint returns 200 only for a service JWT carrying the approved scope; a user token
   (including SUPER_ADMIN) or a service token without the scope gets 403; a missing/invalid token
   gets 401 (D-38 behaviour).
2. Member lookup for a user with no active membership returns `active=false` and no roles (not an
   error), so callers fail closed; a REVOKED role is never returned (§110 "authorization validates
   membership").
3. Building status (ACTIVE, ONBOARDING, SUSPENDED, …) is returned so consumers enforce read-only on
   SUSPENDED (existing `requireWritable` semantics).
4. Unit lookup returns 404 for a unit that does not belong to `{b}`; ownership returned is only the
   currently effective period(s) with share percentage (§19.1), never another building's data
   (§110 isolation test).
5. Member list supports role filter and paging (§118) and is ordered deterministically.
6. Each request is logged with caller service subject and trace id; no secrets or phones logged.
7. Contract documented in OpenAPI under `contracts/openapi/` and covered by a consumer-facing
   contract test.

## Tasks
| Lane | Proposed task | Size |
|---|---|---|
| BE | auth-service: multi-client service credentials + scope config | S |
| BE | building-service: member lookup + member list endpoints | M |
| BE | building-service: unit + current ownership endpoint | S |
| BE | OpenAPI contract + shared client library snippet for consumers (optional) | S |
| QA | Scope/401/403 matrix, isolation, revoked-role, suspended-building tests | S |

## Dependencies
- STORY-001 (permissions in response). Answers: Q-BOS-013-07 (blocking, technical approval of
  D-38 amendment), Q-BOS-013-08 (caching guidance for consumers), Q-BOS-013-11.
- Unblocks BOS-003 Q-08, BOS-004 Q-31, BOS-006 Q-02, BOS-007 Q-03, BOS-008 Q-04.

## Out of Scope
- Consumer adapters and caches inside other services; JWT building-role claims (ADR-F4-001).

## References
BRD §5, §94, §104, §106, §110, §118, §19.1; D-38; ADR-F4-001; `DirectoryBuildingController`,
`ServicePrincipal`.
