# STORY-007: Invoice adjustments, void and overdue detection

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
An operator can correct an issued rent invoice through explicit adjustments or a void, and
unpaid invoices past due are flagged overdue automatically, without ever silently editing
financial history.

## Scope
- `RentAdjustment` (§8.3) via `Add Adjustment` on Invoice Detail (§62); discount and late-fee
  lines (§21) applied as adjustments per Q-BOS-003-25/27.
- Void an invoice (`VOID`, §21) with reason.
- Scheduled overdue detection (§112 "Rent overdue detection") → `OVERDUE` (§21) and
  `rent.invoice.overdue` via outbox (§8.3).
- Flutter: Add Adjustment and Void on Invoice Detail (mobile/24); Overdue tab / Tenant List
  "Rent overdue" filter populated (§55, §61).

## Acceptance Criteria
1. An adjustment is a new appended record (amount, type, reason, actor, timestamp) linked to
   the invoice; the invoice total/outstanding reflect it; existing lines are never
   overwritten (§113 #6 "append-oriented", #10).
2. An adjustment cannot make the payable amount negative; currency is BDT (§113 #9).
3. Void requires a reason, is audited, and is rejected if the invoice has allocations
   (§113 #5 "Paid invoice cannot become unpaid without reversal/adjustment event"); a voided
   invoice no longer blocks regenerating that period (STORY-005 AC2).
4. Finalized invoices are never deleted (§119, §144 "Delete finalized financial records").
5. The overdue job marks `ISSUED`/`PARTIALLY_PAID` invoices with outstanding > 0 past the
   threshold of Q-BOS-003-26 as `OVERDUE` once, publishes `rent.invoice.overdue` once per
   invoice, and is safe to re-run/concurrent (§112).
6. Late fee is applied only as defined by Q-BOS-003-25; until decided, no automatic late fee
   is added.
7. Adjustment and void require a write role (Q-BOS-003-04) and `Idempotency-Key`; replay
   creates nothing new (§16 financial mutations).
8. Concurrent adjustments on one invoice return `VERSION_CONFLICT` (§129 "Invoice").

## Tasks
Proposed task lanes:
- BE/DB: rent_adjustment table, add-adjustment and void use cases with idempotency — M
- BE: overdue job + `rent.invoice.overdue` contract/outbox — S
- Mobile: Add Adjustment sheet, Void confirm, overdue states — S
- QA: append-only/void-with-allocation tests, job re-run test — S

Size: **M** (uncertainty: Q-BOS-003-25/26/27).

## Dependencies
- STORY-006 (Invoice Detail screen), STORY-005.
- BOS-004 for allocations (void guard is meaningful once allocations exist; until then
  invoices have none).
- Questions: Q-BOS-003-25, -26, -27.

## Out of Scope
- Payment reversal (§24) → BOS-004.
- Overdue reminders (§115 "3 days overdue") → BOS-007.

## References
BRD §8.3, §16, §21, §55, §61, §62, §112, §113, §119, §129, §144.
