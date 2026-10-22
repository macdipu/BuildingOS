# STORY-007: Reports screen and core report views

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Authorized users open the Reports screen and view the BRD-defined core financial reports with filters
and pagination (§86, §116; §146 criterion 22).

## Upstream event sources
- rental-service (BOS-003): `rent.invoice.created`, `rent.invoice.overdue`, `lease.activated`, `tenant.created`
- finance-service (BOS-004): `payment.allocated`, `payment.reversed`, `maintenance.invoice.created`,
  `expense.created`, `expense.approved`
- work-service (BOS-006): `work.completed` (Expense Report "Work order" column)
- building-service: `unit.*`, `ownership.*`

## Scope
- Report row projections for the four §116 reports: Rent Collection (Unit, Tenant, Expected, Paid,
  Outstanding, Last payment, Status), Owner Statement (Unit, Rent billed, Rent received, Maintenance due,
  Adjustments), Expense (Date, Category, Vendor, Description, Amount, Work order, Status), Maintenance
  (Unit, Billing month, Billed, Collected, Due).
- `GET /api/v1/reports/buildings/{buildingId}/{reportType}` with §86 filters (Date range, Building, Unit,
  Owner, Tenant) and §118 pagination.
- Flutter Reports screen (no mockup; BRD §86 + DESIGN.md): report cards, filter sheet, `View`.
  Other §86 cards per Q-BOS008-05.

## Acceptance Criteria
1. Each report returns exactly the §116 columns; values derive from projections only (§8.9).
2. Filters narrow results; Building filter offers only buildings where the caller holds `report.read` (§110, §137).
3. OWNER sees only "owner reports" for owned units (§138); TENANT has no Reports access unless granted (§138 TENANT has no `report.read`).
4. Lists paginate with `page/size/sort` and return §118 meta.
5. Owner Statement totals reconcile with the owner dashboard for the same period (§111 vs §116).
6. Expense Report "Status" uses the §26 enum (C-8).
7. Report viewing is not audited unless Q-BOS008-07 says so (§114 lists only "Report export optionally").

## Tasks
Proposed lanes:
- BE/DB: four report projections, report query use cases (one per report, docs/ARCHITECTURE.md rule 3) — L
- Mobile: Reports screen, filter sheet, report table view — M
- QA: column contract tests, owner/tenant scoping, pagination, reconciliation with dashboard — M
Size: **L** (uncertainty: medium).

## Dependencies
STORY-001; STORY-002 (owner data), STORY-005 (maintenance/expense data); BOS-003/004; BOS-006 for work-order column. Q-BOS008-05, Q-BOS008-07.

## Out of Scope
Export/share (STORY-008); six-month reports (§8.9) unless Q-BOS008-05 adds them.

## References
BRD §8.9, §26, §86, §110, §114, §116, §118, §137–138, §146 (22); UI-INDEX "No mockup" (§86); gaps/mobile-ops-community.md.
