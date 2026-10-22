# STORY-006: Maintenance fee dashboard, owner dues view, overdue detection

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
Committee/admin users see billed, collected, outstanding and collection % for a billing month with
Due/Paid/Overdue tabs; owners see their own units' maintenance dues; overdue invoices are detected
daily and announced as events.

## Scope
- Queries: `GetMaintenanceSummaryQuery` (billed, collected, outstanding, collection %), invoice list by
  tab; owner "my dues" query scoped by current ownership from building-service.
- Scheduled job: maintenance overdue detection (§112), single-execution or idempotent across instances.
- Event: `maintenance.invoice.overdue` (§8.4) via outbox.
- Flutter: Maintenance Fee Dashboard (§66, mobile/28): cards Billed, Collected, Outstanding, Collection %;
  tabs Due, Paid, Overdue; buttons `Generate Monthly Fees` (→ STORY-001), `Record Payment` (→ STORY-002),
  `Export Report` (hidden/disabled until BOS-008, Q-BOS-004-23). Owner dues list under Owner `Payments`
  tab (§37, C-1).

## Acceptance Criteria
1. Card figures are computed from invoices and allocations, not raw payment totals (§21 "Use
   allocations"): billed = Σ invoice totals, collected = Σ active (non-reversed) allocations, outstanding =
   billed − collected, collection % = collected / billed (§66).
2. Reversed payments are excluded from collected immediately after reversal (§24).
3. The overdue job marks invoices overdue per the rule in Q-BOS-004-24, emits one
   `maintenance.invoice.overdue` per invoice per transition (no duplicate on re-run), and is safe with
   multiple service instances (§112).
4. An owner sees only invoices of units they currently own (Q-BOS-004-29); a user without building
   membership sees nothing (§4 OWNER "View maintenance dues", §5).
5. Summary query stays inside finance-service; no cross-service DB read (§9, docs/ARCHITECTURE.md rule 6).
6. `Export Report` performs no export in this story (BOS-008).

## Tasks
Proposed lanes only — no TASK files yet.
- BE: summary/tab queries, owner-dues query, overdue job + event — M
- Mobile: dashboard (mobile/28), owner dues list — M
- QA: figure math incl. reversals, job idempotency, owner isolation — S
- Size: **M**.

## Dependencies
STORY-001, STORY-002 (STORY-004 for reversal exclusion test). Q-BOS-004-23, -24, -29.

## Out of Scope
Treasury Balance, Reserve Sinking Fund, bank Live Sync panel, "Exempt / Retained" tab (mobile/28 UI-only;
Q-BOS-004-27); reporting-service projections and dashboards §43–47 (BOS-008); overdue notifications (BOS-007).

## References
BRD §4, §9, §21, §24, §37, §66, §112; UI-INDEX mobile/28, C-1; gaps/mobile-rental-finance.md §28.
