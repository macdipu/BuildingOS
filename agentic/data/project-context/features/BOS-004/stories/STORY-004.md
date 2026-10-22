# STORY-004: Payment reversal

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
An authorized user reverses a recorded payment with a reason; the payment becomes `REVERSED`, its
allocations and any advance credit are reversed, affected invoices reopen, and nothing is deleted.

## Scope
- Use case: `ReversePaymentUseCase`; API `POST /api/v1/payments/{id}/reverse` with `Idempotency-Key`
  (§16, §18).
- Domain: payment status `REVERSED` + `reversal_reason`, `reversed_by`, `reversed_at` (§24); reversing
  allocation entries (append, not update-in-place; §113.6); reversing advance-ledger entry.
- Flutter: Payment Reversal screen (§65, mobile/27): Reason, Notes; displays Original payment, Impact on
  invoices, Advance effect; buttons `Confirm Reversal`, `Cancel`; explicit confirmation step.

## Acceptance Criteria
1. Reversal sets `status = REVERSED`, `reversal_reason`, `reversed_by`, `reversed_at`; the payment row and
   its original allocations remain readable (§24, §113.6–7).
2. All allocations of the payment are reversed and each affected invoice's due/status is recomputed from
   remaining allocations (§21, §24); a PAID invoice becomes unpaid only through this reversal event
   (§113.5).
3. Advance credited by the payment is reversed (§65 "Advance effect"); behaviour when that advance was
   already consumed follows Q-BOS-004-13.
4. A reversed payment cannot be reversed again (`PAYMENT_ALREADY_REVERSED`, §128) and cannot be allocated
   (§113.4, also enforced in STORY-005).
5. The screen shows original payment, per-invoice impact and advance effect before confirmation (§65);
   reason is mandatory (Q-BOS-004-14).
6. Only callers with the reversal permission in the active building may reverse (§64 "permission
   controlled", §65 "Require confirmation and permission"; Q-BOS-004-03); others 403; cross-building 404.
7. Idempotent replay returns the same reversal result with no second set of reversing entries (§16).
8. Same transaction writes an audit row (§114 "Payment reversal", actor + timestamp §113.10) and outbox
   events `payment.reversed` and `advance.balance.changed` when applicable (§8.4, §15).
9. Concurrent reversal vs. new allocation on the same payment/invoice resolves via optimistic locking with
   409 `VERSION_CONFLICT` (§129).

## Tasks
Proposed lanes only — no TASK files yet.
- BE/DB: reversal columns, reversing entries, use case, events — M
- Mobile: reversal screen (mobile/27) from payment detail — S
- QA: invariants 4/5/7, double-reversal, advance-consumed case, idempotency — S
- Size: **M**.

## Dependencies
STORY-002 (API); STORY-003 for the UI entry point. Q-BOS-004-03, -13, -14, -15.

## Out of Scope
Partial reversal / "Partial Adjustment" and dual-signoff protocol (mobile/27 UI-only; §125 complex workflow
engine deferred); back-office `SUPPORT_REVERSE_PAYMENT` relay (D-36, separate CR); refunds (no BRD rule).

## References
BRD §16, §21, §24, §64, §65, §113, §114, §125, §128, §129; UI-INDEX mobile/27;
gaps/mobile-rental-finance.md §27; BOS-010 D-36.
