# BOS-010 — Feature and scope

Status: INTAKE. Full restart requested by the operator (2026-09-22) after the BRD was
revised to redefine BuildingOS as a multi-tenant SaaS platform. This work item
supersedes [BOS-001](../BOS-001) and [BOS-002](../BOS-002); see
[context-index.yaml](../../context-index.yaml) for the supersession record. No prior
BOS-001/BOS-002 decision is silently reused without re-checking it against the revised
BRD.

## Source

[BuildingOS BRD](../../BuildingOS_BRD_Agentic_Development.md), revised 2026-09-22.
Document-first input mode.

## Why a restart, not a continuation

The BRD revision is not additive to BOS-002's scope — it changes the identity/building
domain model itself:

- Users are explicitly global (§110.1); platform roles (`SUPER_ADMIN`, `PLATFORM_ADMIN`,
  `ONBOARDING_AGENT`, `SUPPORT_AGENT`, `SUBSCRIPTION_ADMIN`, §4.1) are now managed
  separately from building membership, which BOS-002 had not modeled.
- A building is no longer created directly; it goes through a
  `BuildingApplication` state machine (`DRAFT → SUBMITTED → UNDER_REVIEW → ... →
  ONBOARDING → ACTIVE`, §110.2, §149.3–149.7) with back-office review/approval.
- Subscription/entitlements are now a first-class SaaS concern (BRD: building-level; **superseded
  2026-09-23 by operator decisions D-22..D-27: per-user subscriptions + one-time building-creation fee**)
  (§8.12, §110.3, §149.15–149.16), enforced by a new `subscription-service`.
- A back-office web application (`buildingos_backoffice_web`, §149.1) is now in scope,
  alongside the existing `user_app` Flutter mobile client.
- The BRD's own MVP definition (§124), MVP success criteria (§146), and Phase 1/1B
  development sequence (§123) were rewritten around this model.

Reusing BOS-002's REQUIREMENTS/DECISIONS as-is would mean building the wrong shape of
identity/building domain and then reconciling it later. This restart re-derives scope
from the current BRD instead.

## Existing baseline at intake, 2026-09-22 (historical; `identity-service` since renamed `account-service`, TASK-004)

- `backend/`: `api-gateway`, `identity-service`, `building-service` exist as a runnable
  local skeleton (BOS-001, commit `463db1a`) — health/readiness, JWT validation at
  gateway and each service, per-service Postgres database/credentials, Kafka
  infrastructure, OpenAPI/Kafka envelope contracts, local CI. **No business
  domain/entities are implemented yet** — the services expose only a read-only
  metadata endpoint (see `TECH-SPEC.md` in BOS-001). This is infrastructure to build
  on, not a domain model to preserve as-is.
- `user_app/`: Flutter starter template; BOS-002's BASELINE.md recorded it as a generic
  shell with placeholder screens and no BRD-shaped auth/API implementation.
- No `subscription-service` or `buildingos_backoffice_web` exist yet; both are new.

## In scope for this work item (subject to REQUIREMENTS/TECHNICAL refinement)

- Global user identity and platform-role model (§4.1 platform roles, §5, §110.1).
- First `SUPER_ADMIN` bootstrap. BOS-002 had already resolved this with the operator
  (deploy-time seed script/config, seed phone `01306999005`, phone+OTP login, no
  admin panel or password) — carried forward as a candidate decision, to be
  re-confirmed once the platform-role model is finalized, not re-litigated from zero.
- Building application, review/approval, and lifecycle (§110.2, §149.3–149.10).
- Building admin assignment, building membership, units, ownership — the original
  BOS-002 domain (§4, §19.1, §48–52), now built against the new lifecycle instead of
  direct creation.
- Assisted onboarding and support-session access (§149.11–149.12), scoped/audited.
- Subscription/entitlement domain and back-office subscription management
  (§8.12, §110.3, §149.15–149.16, §149.23 MVP boundary — no automated billing).
- Back-office console application surface and its core screens (§149.1–149.2,
  §149.5, §149.17–149.18) for the roles/workflows above.
- Phone-number OTP login as the working interpretation (per BOS-002 D-01); Google
  sign-in remains deferred pending optional clarification, unchanged from BOS-002.

## Explicitly out of scope (unaffected by this restart, per BACKLOG.md milestones)

Rental/tenant/lease (BOS-003), payment/financial transactions (BOS-004), offline
sync (BOS-005), work orders (BOS-006), announcements/notifications (BOS-007),
reporting/dashboards (BOS-008), and security/load/release hardening (BOS-009).
Also out of scope per BRD §149.23: automated subscription billing/payment-provider
integration, organization-level consolidated billing, and (per §125/new exclusions)
promotional campaigns and advanced support ticketing/CRM.

## Planning classification

Provisional: **EPIC**, not a single story/task. This work item spans a new global
identity/platform-role model, a new building-lifecycle state machine, a new
subscription domain (new service), and a new client application (back-office web),
each independently testable and independently valuable. Recommend decomposing into
features during PLANNING (e.g. identity & platform roles; building
application/lifecycle & onboarding; units & ownership; subscription & entitlements;
back-office console) rather than one monolithic implementation task. This
classification, and whether full Sprint Planning is warranted, is confirmed by the
INTAKE/PLANNING stages, not assumed here.
