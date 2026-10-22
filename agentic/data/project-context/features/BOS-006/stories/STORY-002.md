# STORY-002: Create and list work orders

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: L (core aggregate + register
screen; uncertainty medium, mostly Q-BOS-006-04/05).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Authorized staff can create work orders (as draft or planned) and browse the building's work
register with filters and search.

## Scope
- WorkOrder aggregate with §27 fields + §73 category, explicit currency, `version`.
- Use cases: CreateWorkOrder (Save Draft / submit), UpdateDraftWorkOrder, ListWorkOrders,
  GetWorkOrder (basic).
- Seeded category list (Q-BOS-006-05).
- Event `work.created` via outbox.
- Flutter feature `work_orders`: Work Order List (§72, mobile/35), Create Work Order (§73, no
  mockup → DESIGN.md), entry from `Work` tab and from Contractor Detail `Create Work` (§76).

## Acceptance Criteria
1. Create form fields: Title, Description, Category, Priority, Planned start, Due date,
   Contractor, Estimated cost (§73); Attachments arrive in STORY-004.
2. Priority ∈ `LOW, MEDIUM, HIGH, CRITICAL` (§27); anything else → 400 `VALIDATION_ERROR` (§128).
3. `Save Draft` stores status `DRAFT` (§73, §8.5); submit without contractor stores `PLANNED` per
   Q-BOS-006-04 default; `Assign` from the create screen is delivered in STORY-003.
4. Selected contractor must belong to the same building and be active, else 422/404 — never leaks
   another building's contractor (§5, §110).
5. `estimated_cost` is non-negative and stored with explicit currency, default BDT (§113.9, §109).
6. Requires `work.create` (§137) for create and `work.read` for list/detail; residents without the
   permission get 403 (§138).
7. List filters: Status, Priority, Contractor, Due date; Search by work title and contractor (§72,
   §117); pagination per §118; `+ Create Work`, Filter, Search buttons (§72).
8. `work.created` written to outbox in the create transaction (§8.5, §15); payload carries
   building id, work order id, priority, due date, version.
9. Create accepts optional `Idempotency-Key` (Q-BOS-006-18); duplicate key returns original.
10. Audit record for create (actor + timestamp) via `/internal/audit` (§114 pattern).
11. Mobile 35 UI-only elements (KPI tiles, SLA countdown, "Technician Roster", chargeable cost tag,
    "Call Tech") are not built (gap 35); List view only — Kanban/Calendar optional, deferred (§72).
12. Contractor Detail gains `Create Work` (pre-fills contractor) and "Current works / Past works"
    lists (§76); Contractor List "Active works" count (§75).

## Tasks
- BE/DB — work_order schema, category seed, create/update-draft/list/get use cases, outbox (L)
- Mobile — `work_orders` feature: list w/ filters+search, create form (M)
- Mobile — contractor detail works lists + Create Work entry (S)
- QA — validation, permission, isolation, filter/search/pagination tests (M)

## Dependencies
STORY-001. Q-BOS-006-04, -05, -16, -18.

## Out of Scope
Status changes past PLANNED (STORY-003), attachments/comments (STORY-004), overdue (STORY-005),
asset link (STORY-006/007), resident intake (STORY-009), meeting-created work (§29, BOS-007).

## References
BRD §8.5, §12, §15, §27, §72, §73, §76, §109, §113, §117, §118, §128, §137, §138;
ui/stitch/gaps/mobile-ops-community.md (35); UI-INDEX C-1.
