# STORY-001: work-service foundation + contractor register

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: L (new deployable; uncertainty
high until Q-BOS-006-01/02/03 are answered).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Stand up `work-service` with building-scoped authorization, entitlement gating, audit and outbox,
and ship the first usable slice on it: the contractor register (§75–76).

## Scope
- New Spring service `backend/work-service` per docs/ARCHITECTURE.md (feature-first `contractor/`,
  `servicemeta/`, `audit/`, `outbox/`; JDBC + Flyway; ArchUnit test), `work_db`, compose + gateway
  routes, Dockerfile, CI verify.
- Authorization adapter: caller's building roles from building-service (Q-BOS-006-02), role →
  permission map (§5, §137).
- Entitlement adapter for `work_orders.enabled` (§149.16, Q-BOS-006-03).
- Contractor entity + ContractorContact (§8.5): name, trade, phone(s) (§75), contacts (§76).
- Use cases: CreateContractor, UpdateContractor, ListContractors (search/filter), GetContractor.
- Event `contractor.created` via outbox (§8.5, §15).
- Flutter feature `contractors`: Contractor List (§75), Contractor Detail (§76), Add/Edit form.
- Back-office: register work-service as audit source + health target.

## Acceptance Criteria
1. `POST /api/v1/buildings/{b}/contractors` (path per Q-BOS-006-16) creates a contractor with
   name, trade, phone and optional contacts; phone accepts `+880` numbers (§109). Returns 201.
2. Only callers with contractor-management permission in building `b` can create/edit
   (COMMITTEE "Contractor management", BUILDING_ADMIN "all building data", §4.1); others get 403
   `ACCESS_DENIED`; no building context → `BUILDING_CONTEXT_REQUIRED` (§128).
3. Tenant isolation: a contractor of building A is never returned, editable, or confirmable as
   existing from building B (404 `RESOURCE_NOT_FOUND`) (§5, §110). Covered by an integration test.
4. Backend rejects contractor/work endpoints with 403 when the entitlement check fails, regardless
   of UI state (§149.16 "Backend is authoritative").
5. List supports `?page=&size=&sort=` with page meta (§118) and search by contractor name (§117);
   rows show Name, Trade, Phone (§75). "Active works" and "Total paid" columns show placeholder
   until STORY-003 / STORY-008 deliver them.
6. Detail shows Contacts and Trade, with `Edit`, `Call` (OS dialer) and `Share Contact` (OS share
   sheet) buttons (§76); `Create Work` appears with STORY-002.
7. `contractor.created` is inserted into `outbox_event` in the same transaction as the contractor
   row and published to `work.events` (§14.2, §15); a failed publish does not roll back the
   contractor; republish is safe to consume twice (event id).
8. Create/update writes an audit record (actor, timestamp, before/after) exposed via
   `GET /internal/audit` and shown in back-office audit aggregation (§114, F6 pattern).
9. Create accepts optional `Idempotency-Key`; a repeated identical request returns the original
   result (Q-BOS-006-18).
10. Contractor removal is a status change (inactive), not hard delete (§119 "Contacts" soft delete).
11. Flutter screens follow DESIGN.md tokens (no mockup); strings via localization keys (§109); the
    entry sits under the `Work` tab for permitted roles (§37, C-1).

## Tasks
Proposed task lanes (no TASK files yet):
- BE/DB — service scaffold, Flyway `work_db`, gateway route, compose, CI (L)
- BE — membership-lookup client + building-service internal endpoint (M; touches building-service)
- BE — entitlement client (S)
- BE — contractor use cases, outbox, audit, `/internal/audit` (M)
- Mobile — `contractors` feature: list, detail, form (M)
- Web — back-office audit/health source registration (S)
- QA — authorization/tenant-isolation/outbox integration tests; Flutter widget tests (M)

## Dependencies
- Q-BOS-006-01, -02, -03, -16, -18.
- BOS-002/BOS-010 identity + membership (delivered); building roles beyond BUILDING_ADMIN/OWNER for
  COMMITTEE access (not yet modelled).

## Out of Scope
Contractor documents (STORY-004), active works (STORY-003), payments/total paid (STORY-008),
ratings/specialties (UI-only, gap 36), vendor marketplace (§3.2).

## References
BRD §4.1, §5, §8.5, §9, §14.2, §15, §75, §76, §109, §110, §114, §117–119, §128, §137, §145, §149.16;
docs/ARCHITECTURE.md; ADR-F4-001; D-22/D-23; ui/stitch/design-system/DESIGN.md.
