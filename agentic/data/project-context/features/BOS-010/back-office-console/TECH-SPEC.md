# TECH-SPEC-F6 (LLD): Back-office console

## Status

APPROVED by operator, 2026-09-25 (service topology, [ADR-F6-001](adr/ADR-F6-001-service-topology.md)).
Refines [REQUIREMENTS.md](REQUIREMENTS.md). Explicit technical approval must still be
recorded on the governed run before IMPLEMENTATION starts.

## Requirement-to-component mapping

| Requirements | Proposed components |
|---|---|
| BOC-01, BOC-02 | `buildingos_backoffice_web` (new React/Next.js project): app shell, nav, auth (JWT against the existing platform issuer), route guards per platform role |
| BOC-03 | Frontend screens calling existing `building-service` building-application/lifecycle endpoints unchanged |
| BOC-04 | Frontend screens calling existing `subscription-service` plan/fee/entitlement endpoints unchanged |
| BOC-05 | New `back-office-service` read/write use cases calling new `auth-service` internal endpoints (`GET /internal/users`, `POST /internal/users/{id}/platform-roles`, `DELETE /internal/users/{id}/platform-roles/{role}`) |
| BOC-06 | New `back-office-service` domain: `AssistedOnboardingSession` entity/repository, start/list/detail/complete/cancel use cases |
| BOC-07 | New `back-office-service` domain: `SupportSession`/`SupportAccessGrant` entity/repository, start/list/detail/end use cases; `ElevatedApprovalRequest` entity for high-risk scopes; approve/deny use cases restricted to `SUPER_ADMIN` |
| BOC-08 | New `back-office-service` read-only use cases: audit query (fans out to each service's existing audited-transition/role-assignment read endpoints or a shared read model — see Audit aggregation below), health aggregation (fans out to each service's `/actuator/health/readiness`) |
| BOC-09 | Frontend dashboard widgets composing BOC-03/04/05/06/07/08's own read endpoints; no new backend metric |

## New service: `back-office-service`

Same shape as every other backend service (Clean Architecture, Flyway, `platform-web`
shared security/error/correlation, `/actuator/health/readiness`, gateway-routed,
port 8084 locally, `back_office_db`). Module added to `backend/pom.xml`.

### Schema (Flyway `V1__back_office.sql`, illustrative — finalize at task-breakdown)

```
assisted_onboarding_session
  id, building_id, assigned_agent_user_id, requested_by_user_id (nullable),
  status (REQUESTED|ASSIGNED|IN_PROGRESS|WAITING_FOR_CUSTOMER|COMPLETED|CANCELLED|EXPIRED),
  access_scope (text[] of the D-10 onboarding scope enum), reason, started_at, expires_at,
  completed_at (nullable), notes (nullable)

support_session
  id, platform_user_id, target_user_id (nullable), building_id (nullable), reason,
  permission_scope (text[] of the D-10 ordinary support scope enum), started_at,
  expires_at, ended_at (nullable)

elevated_approval_request
  id, support_session_id (fk), requested_scope (one of the D-10 high-risk scopes),
  status (PENDING|APPROVED|DENIED), approved_by (nullable, must be SUPER_ADMIN),
  requested_at, decided_at (nullable), decision_reason (nullable)
```

Every row's status transition is an audited event (BOC-08 reads these, doesn't
duplicate them elsewhere).

### `auth-service` additions

- `GET /internal/users?query=&role=&page=` — list users with phone, platform roles,
  building-membership summary counts. Internal, same auth posture as
  `/internal/users/provision` (relayed bearer token, target service's own JWT/role
  check — `SUPER_ADMIN`/`PLATFORM_ADMIN` only, per ID-04).
- `GET /internal/users/{id}` — one user's detail.
- `POST /internal/users/{id}/platform-roles` / `DELETE .../platform-roles/{role}` —
  assign/revoke, `SUPER_ADMIN` only (ID-04: a building admin cannot self-grant a
  platform role; this extends the same rule — no platform role can self-grant another).
  Every call writes an audited row in `auth-service`'s own audit table (AU-01).

### Audit aggregation (BOC-08)

Each service already writes its own audited-transition rows (building-service:
application/building/membership transitions per AP-04/AU-01; auth-service: the new
role-assignment audit above; back-office-service: its own session/approval events).
`back-office-service` does **not** copy this data into its own tables. It exposes a
read API that fans out to a new small internal read endpoint on each service
(`GET /internal/audit?since=&entity=`) and merges/sorts the results for the back-office
UI. This keeps each service the single source of truth for its own audit trail
(no dual-write, no drift) at the cost of a fan-out call per Audit Logs page load —
acceptable for an admin screen, not a hot path.

### `api-gateway` additions

New pass-through route group for `back-office-service`, same pattern as
`building-api`/`subscription-api` in `GatewayRouteConfig.java`:
`/api/v1/platform/backoffice/**` → `BACK_OFFICE_SERVICE_URL`.

## `buildingos_backoffice_web`

New top-level directory (sibling to `user_app`, `backend`, `infra`), a separate
Next.js/React project — not part of the `user_app` Flutter codebase or its Clean
Architecture layer rules (those apply to Flutter/Java only; this is deliberately a
different stack per D-09). Authenticates against the same platform JWT issuer as
`user_app` (`JWT_ISSUER`/`JWT_JWK_SET_URI`, §149.1's "own platform permission checks"
means route guards checking the decoded token's `platform_roles` claim, not a
separate identity system).

## Mutation/authorization pattern for support/onboarding sessions

For any action performed "under" a `SupportSession`/`AssistedOnboardingSession`:
1. `back-office-service` validates the session is active (not expired/ended) and the
   requested action's scope is within the session's granted `permission_scope`/`access_scope`.
2. If the action's scope is high-risk (D-10's four scopes) and has no `APPROVED`
   `ElevatedApprovalRequest`, reject with an explicit "pending approval" response —
   never silently downgrade or auto-approve.
3. Only then does it relay the call (with the acting user's own bearer token) to the
   owning service (`building-service` for unit/membership/ownership actions,
   `subscription-service` for financial-export/payment-reversal, `auth-service` for
   Building-Admin removal via its existing membership-revocation-equivalent, if that
   exists, or building-service's membership revoke otherwise).
4. The target service's own JWT validation and platform-role check remain the real
   authorization boundary — `back-office-service`'s scope check is additive, not a
   replacement.

## Out of scope for this TECH-SPEC

Exact Next.js routing/component structure, exact `back-office-service` REST payload
shapes, and the precise mapping from "Building-Admin removal" to an existing or new
building-service endpoint are task-breakdown-level detail, not architecture — resolve
them per task, not invented wholesale here.
