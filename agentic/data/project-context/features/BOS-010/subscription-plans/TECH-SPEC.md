# BOS-010 F5a — Technical design: subscription-service (revenue foundation)

Status: IMPLEMENTED (revision 2, approved 2026-09-23) — see [REVIEW.md](REVIEW.md) for deviations (jsonb billing cycles, `/me/plans`).
Requirements: [REQUIREMENTS.md](REQUIREMENTS.md) RV-01..RV-11. Architecture:
[docs/ARCHITECTURE.md](../../../../../../docs/ARCHITECTURE.md). Mirrors existing services.

## 1. Service scaffolding (RV-01) — unchanged from revision 1

Module `backend/subscription-service` (`com.buildingos.subscription`, port 8083, platform-web,
JDBC + Flyway, ArchUnit copied). `infra/docker/postgres/init/03-subscription-db.sh`,
`SUBSCRIPTION_DB_PASSWORD` in `.env.example`/compose/`verify-platform.sh` (migrate twice,
3-way DB isolation). Gateway `SUBSCRIPTION_SERVICE_URL` routes for every path below plus
metadata route. OpenAPI paths in `contracts/openapi/platform.yaml`.

## 2. Changeability (D-27) — how each likely change stays cheap

| Likely change | Where it lives | Cost of change |
|---|---|---|
| Which features are free | `free_tier` row (admin API) | Data edit |
| Plan contents / new plans | `subscription_plan` rows | Data edit |
| Fee amount, fee on/off | `fee_schedule` row (`amount`, `required`) | Data edit |
| New one-time fee (e.g. unit onboarding) | new `fee_schedule` code + reference type enum value | Small code change |
| Building-level (or org-level) subscriptions | `subscription.subject_type` already stored; add enum value + resolver | New resolver, no migration |
| Real payment gateway | `PaymentMethod` enum + new adapter behind `PaymentRecorder` port | New adapter |
| New feature key | `Feature` catalog enum (D-16) | One enum entry |

Entitlement resolution is an application port `EntitlementResolver` with implementation
`FreeTierPlusSubscriptionResolver`: effective = free tier ⊕ plan entitlements of the subject's
active subscription (plan booleans OR free booleans; plan limits override free limits). Subject
lookup goes through `SubscriptionSubject(type, id)`, so a building/org model is an added
resolver, not a rewrite.

## 3. Feature packages (one use case per action)

- `catalog` — `domain/model`: `Feature` enum (RV-02 keys + type), `Entitlements` value object
  (validation), `BillingCycle`.
- `freetier` — use cases `getfreetier`, `updatefreetier`.
- `plan` — `SubscriptionPlan`, `PlanStatus`, `PlanCode`; use cases `createplan`, `updateplan`,
  `retireplan`, `getplan`, `listplans`.
- `subscription` — `Subscription`, `SubjectType` (`USER`), `GrantSource` (`ADMIN`,
  `SELF_SERVICE`); use cases `grantsubscription` (admin), `selfsubscribe`,
  `getusersubscription`, `getmyentitlements`.
- `fee` — `FeeSchedule`, `FeeCode` (`BUILDING_CREATION`), `PaymentRecord`, `PaymentMethod`
  (`MANUAL`), `PaymentReference(type=BUILDING_APPLICATION, id)`; use cases `getfeeschedule`,
  `updatefeeschedule`, `recordpayment`, `getfeestatus`.
- `audit` — `AuditRecorder` port + JDBC adapter; called inside each mutating transaction.

## 4. Schema (`V2__revenue_foundation.sql`, forward-only)

```sql
free_tier(id smallint pk check (id = 1), entitlements jsonb not null, updated_at timestamptz)
  -- seeded once: {"maintenance.enabled": true}  (D-23; editable afterwards)
subscription_plan(id uuid pk, code varchar(64) unique not null, name varchar(200) not null,
  status varchar(16) not null, billing_cycles varchar(16)[] not null, self_service boolean not null,
  entitlements jsonb not null, created_at timestamptz, updated_at timestamptz)
subscription(id uuid pk, subject_type varchar(16) not null, subject_id uuid not null,
  plan_id uuid not null references subscription_plan, status varchar(16) not null,
  billing_cycle varchar(16) not null, granted_by varchar(16) not null, started_at timestamptz,
  created_at timestamptz, updated_at timestamptz)
  -- unique (subject_type, subject_id) where status = 'ACTIVE'
fee_schedule(code varchar(32) pk, amount numeric(12,2) not null check (amount >= 0),
  currency char(3) not null, required boolean not null, updated_at timestamptz)
  -- no row seeded: amount is operator data (D-24)
payment_record(id uuid pk, fee_code varchar(32) not null references fee_schedule,
  reference_type varchar(32) not null, reference_id uuid not null, amount numeric(12,2) not null,
  currency char(3) not null, method varchar(16) not null, external_reference varchar(128),
  paid_on date not null, recorded_by uuid not null, recorded_at timestamptz)
audit_event(id uuid pk, actor_user_id uuid not null, action varchar(64) not null,
  entity_type varchar(32), entity_id varchar(64), before jsonb, after jsonb, occurred_at timestamptz)
```

## 5. Behavior

- Auth: platform endpoints require `SCOPE_platform_role.{SUPER_ADMIN|PLATFORM_ADMIN|SUBSCRIPTION_ADMIN}`;
  `/api/v1/me/*` any authenticated user, subject = token `sub`. 403/401 via platform-web.
- Fee status: `GET /api/v1/platform/fees/BUILDING_CREATION/status?referenceType=BUILDING_APPLICATION&referenceId=…`
  → `SETTLED` | `UNPAID` | `NOT_REQUIRED`; missing `fee_schedule` row → 409 `FEE_NOT_CONFIGURED`
  (fail closed, like D-08's "no plan" rule). Building-application approval (next slice) calls this.
- Payment currency must equal the schedule currency; partial payments sum.
- Error codes (`ApiError`): `PLAN_NOT_FOUND` 404, `PLAN_CODE_TAKEN` 409, `PLAN_RETIRED` 409,
  `PLAN_NOT_SELF_SERVICE` 403, `BILLING_CYCLE_NOT_OFFERED` 409, `ALREADY_SUBSCRIBED` 409,
  `SUBSCRIPTION_NOT_FOUND` 404, `FEE_NOT_CONFIGURED` 409, `CURRENCY_MISMATCH` 409;
  validation → existing 400 `INVALID_REQUEST`.
- Concurrency: grant/self-subscribe rely on the partial unique index → one winner, loser
  `ALREADY_SUBSCRIBED`.

## 6. Tests

Unit: `Entitlements` validation, resolver merge (free only / free+plan / limit override),
fee status rules, each use case with fakes. Integration (Testcontainers): plan CRUD + retire,
admin grant vs self-subscribe rules, concurrent subscribe → one, `/me/entitlements` per user
(owner subscription does not affect tenant), free-tier edit applies immediately, fee not
configured → 409, manual payments → SETTLED, audit rows, 401/403. ArchUnit, gateway routes,
`verify-platform.sh`, `check-contracts.py`, `mvn -B verify`.

## 7. Out of scope / risks

No real charging, prices, trials, renewal/expiry, cancel/suspend; no enforcement inside other
services yet (they will call `/me/entitlements` or a later internal API); no UI; no Kafka.
Only `SUPER_ADMIN` exists today (no role-management API) — tests mint tokens for other roles.
User ids are not validated against auth-service (separate DB); ids come from JWT `sub`.

Estimate: agent ~3 h across 1–2 sessions; human ~20–30 min review + API smoke.
