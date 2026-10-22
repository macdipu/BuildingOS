# STORY-007: Expense lifecycle (register, add, detail, status enum, receipt document)

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
Committee/finance users record common building expenses with a receipt document, move them through the
BRD status lifecycle with every transition audited, and browse/filter them.

## Scope
- Entities: `Expense` with the §26 field list (id, building_id, category_id, title, description, amount,
  expense_date, payment_method, vendor, work_order_id optional, receipt_document_id optional, created_by,
  approved_by optional, status), `ExpenseStatusTransition`, `ExpenseCategory` (§8.4, §89 Finance).
- Use cases: create (DRAFT), edit, submit, approve, reject, mark paid, void; list/get; category CRUD.
- Documents: receipt upload via signed-URL flow (§131) behind a storage port (Q-BOS-004-18).
- Events: `expense.created`, `expense.approved` (§8.4) via outbox.
- Flutter: Expense List (§69, mobile/31) — filters Date, Category, Status, Vendor, Work order; cards This
  month, Approved, Pending; buttons `+ Add Expense`, `Export` (disabled until BOS-008), Search. Add Expense
  (§70, mobile/32) — Title, Category, Amount, Date, Vendor, Method, Work order optional, Description,
  Receipt photo/document; `Save Draft`, `Submit`, `Cancel`. Expense Detail (§71, mobile/33) — Amount,
  Status, Vendor, Created by, Approved by, Receipt, Related work; `Edit` when allowed, `Approve`, `Reject`,
  `Mark Paid`, `Void`.

## Acceptance Criteria
1. Status values are exactly DRAFT, SUBMITTED, APPROVED, REJECTED, PAID, VOID (§26, C-8); the UI shows no
   "Settled" / "Approved & Paid" labels.
2. Allowed transitions follow the table resolved in Q-BOS-004-16; any other transition → 409/422 with a
   §128-style code; `Edit` is offered only in states where Q-16 allows it (§71 "Edit when allowed").
3. Every status transition writes a transition row and an audit row with actor + timestamp in the same
   transaction (§71 "All status transitions audited", §114 "Expense approval", §113.10).
4. Expenses are never deleted; VOID is the terminal cancellation (§26, §113.6).
5. `approved_by` is set on approval; `expense.created` on create and `expense.approved` on approval are
   emitted via the outbox (§8.4, §15).
6. Create is idempotent with `Idempotency-Key` (§16 "Create expense").
7. Amount > 0 and currency explicit (§113.1/9 applied to expenses); category must belong to the same
   building.
8. Receipt document is optional (§26 `receipt_document_id optional`); upload uses a signed upload URL and
   the expense stores only the document id (§131); download uses a short-lived signed URL (§93).
9. `work_order_id` is optional and handled per Q-BOS-004-19 until BOS-006 exists.
10. List filters Date, Category, Status, Vendor (+ Work order once available) and the three cards work
    server-side (§69).
11. Authorization per Q-BOS-004-03 (§4 COMMITTEE "Common expenses", ACCOUNTANT "Expense entry");
    cross-building access → 404; mutations blocked on SUSPENDED buildings (Q-30). Entitlement per Q-21.

## Tasks
Proposed lanes only — no TASK files yet.
- BE/DB: expense, transition, category tables; state machine use cases; events — L
- BE: document storage port + S3/MinIO adapter, signed URLs — M
- Mobile: register (31), add voucher (32), detail (33) — L
- QA: transition matrix, audit completeness, document access isolation — M
- Size: **L** (uncertainty medium; approval workflow open).

## Dependencies
STORY-001 only (parallel to STORY-002..006). Q-BOS-004-03, -16, -17, -18, -19, -20, -21, -23. BOS-006 for
work-order linkage (later).

## Out of Scope
Budget burn / runway / sinking reserve cards, vendor P&L link (mobile/31); Trade License/BIN field and named
dual-signoff routing (mobile/32/33; §125 complex workflow engine deferred); multi-step configurable
approval chains; expense export (BOS-008); OCR receipt extraction (§3.2).

## References
BRD §4, §8.4, §16, §26, §69–71, §89, §93, §113, §114, §125, §131; UI-INDEX mobile/31–33, C-8;
gaps/mobile-rental-finance.md §31–33; BOS-010 D-28 (storage pattern).
