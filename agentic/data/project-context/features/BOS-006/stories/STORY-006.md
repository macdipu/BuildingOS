# STORY-006: Basic asset register

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M (CRUD on existing service;
uncertainty low-medium, Q-BOS-006-06/07).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Staff register building assets (generator, pump, lift, CCTV, fire extinguisher, transformer, solar
equipment) and view their metadata, warranty and supplier.

## Scope
- Asset entity with §30 fields in the owning service (Q-BOS-006-07 default work-service).
- Use cases: CreateAsset, UpdateAsset, ListAssets, GetAsset, RetireAsset (status change).
- Asset documents reuse STORY-004 storage if STORY-004 has shipped (`Add Document`, §84);
  otherwise the button is hidden until then.
- Flutter feature `assets`: Asset List (§83), Asset Detail (§84), Add/Edit form — no mockup, plan
  from DESIGN.md.

## Acceptance Criteria
1. Fields: asset_code, name, category, purchase_date, warranty_expiry, supplier, status,
   maintenance_interval, last_service_date, next_service_date (§30).
2. Category from §30 list (Generator, Water pump, Lift, CCTV, Fire extinguisher, Transformer, Solar
   equipment) + OTHER, unless the operator chooses configurable categories.
3. `asset_code` unique within a building (409 on duplicate); different buildings may reuse a code
   (§110 isolation).
4. Status per Q-BOS-006-06; assets are retired, not deleted (§119).
5. List shows Asset code, Name, Category, Status, Next service with `+ Add Asset`, Filter, Search
   (§83); pagination §118.
6. Detail shows metadata, warranty, supplier, next service, with `Edit`, `Add Document` (§84);
   "Work history" and `Create Maintenance Work` arrive in STORY-007.
7. Permissions: create/edit with work-management permission (COMMITTEE / BUILDING_ADMIN, §4.1);
   read with `work.read` (§137) — BRD has no `asset.*` permission; confirm with Q-BOS-006-02.
8. Create/update/retire are audited (actor + timestamp) via `/internal/audit`.
9. Cross-building access → 404 (§5, §110).

## Tasks
- BE/DB — asset schema + use cases + audit (M)
- Mobile — `assets` feature: list, detail, form (M)
- QA — uniqueness, isolation, permission tests; widget tests (S)

## Dependencies
STORY-001 (service, authorization). Optional STORY-004 for documents. Q-BOS-006-06, -07, -18.

## Out of Scope
Reminders and maintenance work (STORY-007), depreciation/accounting (§3.2 accounting integrations),
advanced/predictive maintenance (§125, §3.2).

## References
BRD §3.1, §4.1, §30, §83, §84, §110, §118, §119, §125, §137; ui/stitch/design-system/DESIGN.md.
