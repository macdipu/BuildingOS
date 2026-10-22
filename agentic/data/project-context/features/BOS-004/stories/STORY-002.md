# STORY-002: Record maintenance payment with allocation, advance credit and server receipt number

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
An authorized user records a maintenance payment for one or more of a unit's open invoices, the system
allocates it oldest-first or as manually entered, moves any excess to the advance ledger, and assigns a
unique server-generated receipt number — all in one transaction.

## Scope
- Use cases: `RecordPaymentUseCase` (§126 chain: validate permission → validate invoices/allocation →
  create payment → create allocation → create advance if needed → create receipt metadata → save outbox
  → commit), `ProposeAllocationQuery` (oldest-first proposal for the UI, §126 step 5).
- Entities: `Payment` (§92 fields: payer, unit, paymentType=MAINTENANCE, amount, currency, paymentMethod,
  paidAt, reference, note, collector; status; `@Version` §129), `PaymentAllocation`, `AdvanceLedger`
  entry, `Receipt` + per-scope sequence (§108).
- API: `POST /api/v1/payments` with `Idempotency-Key` (§92 request shape); payment methods = §8.4 enum
  (CASH, BKASH, NAGAD, BANK_TRANSFER, CARD, CHEQUE, OTHER).
- Finance settings: building receipt prefix (§89 Finance "Receipt numbering"; Q-BOS-004-04).
- Flutter: Maintenance Payment screen (§68, mobile/30): Owner, Unit(s), Invoice(s), Amount, Payment method,
  Reference, Date; allocation UI per C-5/§63 (Auto allocate oldest first / Manual allocation toggle);
  confirmation summary (Received, Allocated, Advance created, Remaining due — §63); buttons `Save Payment`,
  `Save & Receipt` (§68). Optional receipt attachment (§63) only if Q-BOS-004-18 is resolved in time.

## Acceptance Criteria
1. Amount must be > 0 (§113.1); currency explicit (§113.9).
2. Sum of allocations ≤ payment amount (§113.2) else `PAYMENT_ALLOCATION_EXCEEDS_AMOUNT` (§128).
3. An allocation can never push an invoice's allocated total above its payable amount (§113.3); allocating
   to a PAID invoice returns `INVOICE_ALREADY_PAID` (§128).
4. Auto mode allocates to the selected invoices in ascending billing month (oldest first, §63); manual mode
   uses the client-supplied per-invoice amounts after server validation. The server, not the client, is
   authoritative (§93 "Never rely on Flutter UI").
5. Any unallocated remainder is credited to the advance ledger as an explicit entry, not as a negative
   invoice (§23, §113.3); `advance.balance.changed` is emitted (§8.4). Ledger scope per Q-BOS-004-11.
6. Invoice status/due update from allocations only (§21 "due = invoice total − allocated payment");
   e.g. 25,000 invoice + 15,000 payment → PARTIALLY_PAID, due 10,000; 20,000 invoice + 50,000 payment →
   PAID + 30,000 advance (§143 B/C math, applied to maintenance).
7. A receipt number is generated server-side in the same transaction, unique per §113.8, formatted from
   building code, payment category, year/month and sequence (§108; C-7); concurrent payments never get the
   same or a skipped-then-reused number (DB-unique constraint + test). Format details per Q-BOS-004-04/05.
8. Idempotency: replaying the same key + body returns the original payment and receipt; no duplicate
   payment, allocation, advance entry or receipt number (§16, §63 "Idempotency mandatory").
9. One transaction writes payment, allocations, advance entry, receipt, audit row (§114 "Payment creation",
   actor + timestamp §113.10) and outbox events `payment.recorded`, `payment.allocated`,
   `receipt.generated` (§126 Kafka).
10. Authorization: only roles with payment-entry permission in the active building (Q-BOS-004-03); invoices
    from another building or unit cannot be allocated (404, tenant isolation). Payer rule per Q-BOS-004-10.
11. Concurrent allocation to the same invoice: one wins, the other gets 409 `VERSION_CONFLICT` (§129).
12. Flutter shows the §63 confirmation summary before submit and the receipt number after success.

## Tasks
Proposed lanes only — no TASK files yet.
- BE/DB: payment, allocation, advance, receipt tables + sequence; Flyway — M
- BE: record-payment use case, allocation policy (auto/manual), receipt-number generator, events — L
- Mobile: maintenance payment screen (mobile/30) with allocation toggle and summary — M
- QA: §113 invariants 1–3/8–10, Scenario B/C math, concurrency, idempotency replay — M
- Size: **L** (uncertainty medium; receipt format and ledger scope open).

## Dependencies
STORY-001. Q-BOS-004-03, -04, -05, -10, -11, -18 (attachment only), -25.

## Out of Scope
"Deposited To" treasury account selector, waived late-fee display (mobile/30 UI-only); payments of type RENT
(STORY-008), SECURITY_DEPOSIT, ADVANCE_RENT, OTHER without an invoice (follow-up); mixed-type payments
(Q-BOS-004-22 note); PDF (STORY-003); reversal (STORY-004).

## References
BRD §8.4, §16, §21–23, §63, §68, §92, §93, §108, §113, §114, §126, §128, §129, §143 B/C; UI-INDEX
mobile/30, C-5, C-7; gaps/mobile-rental-finance.md §30.
