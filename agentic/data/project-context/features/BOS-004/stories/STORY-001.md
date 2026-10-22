# STORY-001: Finance service foundation + maintenance fee generation (preview / generate)

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
An authorized building finance user can preview and then generate one month's maintenance invoices for a
chosen unit scope and rate policy, with duplicates for the same billing period prevented. Ships the
finance-service skeleton (finance_db, building-context authorization, idempotency, audit, outbox) that every
later story reuses.

## Scope
- New `backend/finance-service` (Clean Architecture, one use case per action, JDBC + Flyway, ArchUnit test,
  readiness endpoint) per docs/ARCHITECTURE.md; gateway routes; local compose entry. Subject to Q-BOS-004-01.
- Cross-cutting kernel inside finance-service: building-context authorization adapter to building-service
  (Q-BOS-004-31), permission checks (§5 "permission checks instead of hard-coding role names"),
  `Idempotency-Key` store (§16), `finance_audit` (§114), `finance_outbox` + publisher to `finance.events`
  (§14–15), money type with explicit currency (§113.9, §109; Q-BOS-004-25).
- Use cases: `PreviewMaintenanceFeesUseCase` (no persistence), `GenerateMaintenanceFeesUseCase`,
  `ListMaintenanceInvoicesQuery`, `GetMaintenanceInvoiceQuery`.
- Entities: `MaintenanceInvoice` (§25: unit_id, billing_month, base_fee, adjustment, previous_due,
  due_date, status, + building_id, currency, version), `MaintenanceGenerationRun`.
- Flutter: `features/finance/` scaffold; Generate Maintenance Fees screen (§67, mobile/29) with fields
  Billing month, Due date, Unit scope, Rate policy, Exclusions; Preview table (Unit, Amount); buttons
  `Preview`, `Generate`, `Cancel`; maintenance invoice list (minimal, no cards — cards are STORY-006).

## Acceptance Criteria
1. `Preview` returns one row per in-scope, non-excluded unit with the computed amount and persists nothing
   (§67 Preview: Unit, Amount; Preview and Generate are distinct actions per §67 buttons).
2. `Generate` creates exactly one maintenance invoice per previewed unit with the §25 fields and the
   building's currency (§25, §113.9). Initial status follows Q-BOS-004-06.
3. Supported rate policies are those resolved in Q-BOS-004-07 from the §25 list; unit size uses
   building-service `area_sqft`, custom rate uses `default_maintenance_rate`. A unit whose required input
   is missing is reported in the preview as not billable, never billed at an invented amount.
4. A second `Generate` for the same building + billing month (+ unit) is rejected with 409 and a §128-style
   code, unless the adjustment mode resolved in Q-BOS-004-08 is explicitly requested (§67).
5. Repeating the same request with the same `Idempotency-Key` returns the original result and creates no
   new invoices; the same key with a different body returns `DUPLICATE_IDEMPOTENCY_KEY` (§16, §128).
6. Generation writes one audit row (actor, timestamp, building, billing month, invoice count, rate policy)
   in the same transaction (§114 "Maintenance fee generation", §113.10).
7. Each created invoice emits `maintenance.invoice.created` via the outbox in the same transaction, with
   `eventId`, `occurredAt`, `buildingId`, `eventVersion` (§8.4, §14.1, §15); a JSON schema is added to
   `contracts/kafka/`.
8. Only callers holding the finance-generation permission in the active building may preview/generate
   (Q-BOS-004-02/03); others get 403; a caller with no membership in building B gets 403/404 for any
   building-B invoice (tenant isolation, §5, §93). Authorization failure of building-service fails closed (503,
   not allow).
9. Mutations are rejected when the building is `SUSPENDED` (existing `BuildingContext.readOnly()`
   pattern; Q-BOS-004-30).
10. Invoice reads are filtered by `X-Building-Id`/path building and return due = total − allocated
    (0 allocations in this story) (§21 rule, applied to maintenance by Q-BOS-004-06).
11. Maintenance features are available on the free tier (BOS-010 D-23); no subscription check blocks them.
12. Flutter: all strings via localization keys (§109); amounts shown in BDT; server errors branch on code
    (§128); preview must be run before `Generate` is enabled.

## Tasks
Proposed lanes only — no TASK files yet.
- BE/DB: finance-service scaffold, gateway route, compose, Flyway V1 (invoice, run, idempotency, audit,
  outbox) — L
- BE: building-service context adapter + permission evaluator — M
- BE: preview/generate/list/get use cases, outbox publisher, Kafka schema, OpenAPI — L
- Mobile: finance feature scaffold, generate screen (mobile/29), invoice list — M
- QA: invariants, idempotency, duplicate-period, isolation, ArchUnit, contract tests — M
- Size: **XL** (new deployable; uncertainty high until Q-01/02/06/07/08 are answered).

## Dependencies
- BOS-010 delivered: building-service units (`area_sqft`, `default_maintenance_rate`), memberships, building
  status; auth JWT.
- Q-BOS-004-01, -02, -03, -06, -07, -08, -09, -25, -30, -31.

## Out of Scope
Itemized budget breakdown and SMS/WhatsApp dispatch toggles from mobile/29 (UI-only, gap file);
"Owner category" rate policy until defined (Q-07); scheduled monthly auto-generation (§112) — follow-up
once manual generation is accepted; dashboard cards (STORY-006).

## References
BRD §5, §8.4, §9, §14–16, §25, §67, §109, §112, §113, §114, §128–130, §145; UI-INDEX mobile/29;
gaps/mobile-rental-finance.md §29; BOS-010 D-23; docs/ARCHITECTURE.md rules 1–8.
