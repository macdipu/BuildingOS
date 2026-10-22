# STORY-005: Committee dashboard and committee finance projection

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Committee members see maintenance collection, outstanding maintenance, monthly expenses, cash/bank
totals, open work orders and budget variance, with governance shortcuts (§47, §111 `committee_finance_view`).

## Upstream event sources
- finance-service (BOS-004): `maintenance.invoice.created`, `maintenance.invoice.overdue`,
  `payment.recorded`, `payment.allocated`, `payment.reversed`, `expense.created`, `expense.approved`
- work-service (BOS-006): `work.created`, `work.started`, `work.completed`, `work.overdue`

## Scope
- Projection `committee_finance_view` with the §111 fields (building_id, month, maintenance_billed,
  maintenance_collected, expense_total, outstanding_total); `work_open_view`.
- Committee shape of the dashboard API.
- Flutter committee dashboard (mockup `mobile/09-committee-dashboard`): cards per §47; buttons Add
  Expense, Generate Maintenance Fees, Create Work Order, Post Announcement, Create Meeting, Open Financial
  Report (§47) — the last opens the STORY-007 Reports screen.

## Acceptance Criteria
1. Month figures use Asia/Dhaka month boundaries and BDT (§109).
2. `expense_total` counts expenses per the rule fixed in Q-BOS008-13 (recommended default: APPROVED and PAID, excluding REJECTED/VOID per §26 statuses, C-8).
3. "Work orders open" counts work orders not COMPLETED/CANCELLED (§8.5 status list) and is its own card (gaps/mobile-core.md 09 BRD-only item).
4. "Cash/bank totals" and "Budget variance" are built only after Q-BOS008-14 / Q-BOS008-15 resolve; until then those cards are hidden, not faked.
5. Only roles with committee/report permission in the building can read the committee shape (§4 COMMITTEE "Reports", §137 `report.read`); others get 403.
6. Buttons render by permission (`expense.create`, `maintenance.invoice.generate`, `work.create`,
   `announcement.create`, `meeting.create`) (§137).
7. Mockup-only governance quorum-voting / "Remind President" is not built (C-13; gaps/mobile-core.md 09).
8. Idempotent consumers; reversed maintenance payment restores outstanding (§16, §24).

## Tasks
Proposed lanes:
- BE/DB: committee finance + work-open projections and query — M
- Mobile: committee dashboard screen — M
- QA: month boundary, expense status filter, reversal, permission checks — S
Size: **M** (uncertainty: high — two cards blocked on undefined BRD rules).

## Dependencies
STORY-001; BOS-004; BOS-006 (work card can ship later, independently). Q-BOS008-13, -14, -15.

## Out of Scope
Budgets as an entity (no BRD definition); meeting/announcement creation flows (BOS-007).

## References
BRD §4 (COMMITTEE), §8.4, §8.5, §26, §47, §109, §111, §137; UI `mobile/09-committee-dashboard`; gaps/mobile-core.md §09; UI-INDEX C-8, C-13.
