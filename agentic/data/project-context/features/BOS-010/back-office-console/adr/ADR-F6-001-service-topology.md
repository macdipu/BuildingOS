# ADR-F6-001 — Back-office service topology

Status: ACCEPTED by operator, 2026-09-25. Decision: new dedicated `back-office-service`
owning session/audit data, calling `auth-service`/`building-service`/`subscription-service`
for everything else. Date: 2026-09-25. Scope: BOS-010 F6 BOC-05..BOC-08.

## Context

F6 needs backend capability that doesn't exist yet: platform-user/role listing and
assignment (BOC-05), assisted-onboarding sessions (BOC-06), support sessions with
elevated approval (BOC-07), and an audit-log/system-health read surface (BOC-08).

`auth-service` already owns `User`/`PlatformRole` since the identity slice (TASK-001,
ID-04). `building-service` already owns buildings/membership/units/ownership and their
audited-transition rows. `subscription-service` owns plans/entitlements/fees. None of
the new F6 entities (`AssistedOnboardingSession`, `SupportSession`/`SupportAccessGrant`,
elevated-approval requests) naturally belongs to any one of them, and BOC-07's support
actions can span building-service, auth-service and subscription-service data
depending on scope.

## Decision

A new microservice, `back-office-service`, with its own `back_office_db`, following the
same architecture (Clean Architecture, Flyway, gateway-routed, JWT-validated) as every
other service. It owns:

- `AssistedOnboardingSession` (BOC-06)
- `SupportSession` / `SupportAccessGrant` and elevated-approval requests (BOC-07)
- An audit-log read/aggregation view over the audited-transition and role-assignment
  records already written by `auth-service`/`building-service`/`subscription-service`
  (BOC-08) — this is a read aggregation, not a new place those services stop auditing
  their own writes.
- System-health aggregation, fanning out to each service's existing
  `/actuator/health/readiness` (BOC-08).

It does **not** own `User`/`PlatformRole` data. BOC-05 (platform-user/role management)
is served by `back-office-service` calling new internal `auth-service` endpoints
(list users, assign/revoke platform role) — mirroring the existing D-29 pattern where
`building-service` calls `auth-service`'s `/internal/users/provision` directly rather
than duplicating user data. `auth-service` keeps sole ownership of `User`/`PlatformRole`,
unchanged from the identity slice.

When a support/onboarding session performs an actual mutation (e.g. edit a unit, revoke
a membership) under its granted scope, `back-office-service` calls the owning service's
existing API directly (same cross-service-call pattern as D-29), after checking the
scope itself. It does not duplicate business data from those services.

## Alternatives considered

1. **Split across existing services** (auth-service for BOC-05, building-service for
   BOC-06/07, a small gateway-level aggregation for BOC-08). Rejected by operator:
   would spread back-office-only concerns across services that otherwise have no
   reason to know about "back office" as a concept, and the audit-aggregation piece
   doesn't cleanly belong to any one of them either.
2. **Everything in building-service** (like F2/F4 did for their own domains). Rejected:
   BOC-07's support-session scope isn't building-specific alone (financial-export and
   payment-reversal scopes touch subscription-service data too), and mixing
   back-office-only session/approval bookkeeping into building-service's existing
   domain would blur that service's boundary.

## Consequences

- A fifth backend service (`platform-web`, `auth-service`, `building-service`,
  `subscription-service`, `api-gateway`, now `back-office-service`) — more topology to
  build, deploy, and reason about locally (compose, gateway routes, JWT config).
- `auth-service` needs new internal endpoints for user listing and role
  assign/revoke, gated the same way `/internal/users/provision` already is (internal,
  not through the gateway's public routes).
- `api-gateway` needs new routed paths for `back-office-service`'s API surface.
- Support-session actions call `building-service`/`auth-service`/`subscription-service`
  by relaying the acting platform user's own bearer token, the same mechanism
  `building-service` already uses for `/internal/users/provision` (D-29) — no separate
  service-account credential to introduce. The target service's own JWT validation and
  platform-role checks (`SUPER_ADMIN`/`PLATFORM_ADMIN` etc.) are the real enforcement
  point; `back-office-service` additionally checks the support/onboarding session's
  granted scope before relaying the call, but never substitutes for the target
  service's own authorization.
