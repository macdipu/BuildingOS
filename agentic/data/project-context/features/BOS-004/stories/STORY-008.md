# STORY-008: Collect rent against rent invoices (Scenarios B/C)

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
A rent collector selects a tenant/unit, sees open rent invoices, enters the amount and method, confirms
the proposed allocation and gets a receipt — full, partial and advance rent all handled (§146 criterion 14).

## Scope
- Consumer: `rent.invoice.created` (and BOS-003 adjust/void events) → `RentInvoiceProjection` in finance_db,
  idempotent by `eventId` (§8.4 Consumes, §14.1 rule 3).
- Reuse `RecordPaymentUseCase` with `paymentType = RENT`, allocation against rent-invoice projections,
  advance ledger for rent (Q-BOS-004-11), receipt category RNT (§108 example).
- Publish `payment.allocated` / `payment.reversed` so rental-service updates invoice settlement state
  (§8.3 Consumes `payment.allocated`; §126 Consumers/Rental) — authority split per Q-BOS-004-22.
- Flutter: Collect Rent (§63, mobile/25): Tenant, Unit, Selected invoices, Amount received, Payment date,
  Method, Reference, Notes, Receipt attachment optional; Auto allocate oldest first / Manual allocation
  toggle; confirmation summary Received / Allocated / Advance created / Remaining due; `Save Payment`,
  `Save & Share Receipt`, `Cancel`. Entry from Manager dashboard / Finance tab (§126 Frontend). Tenant
  "Payments" tab: own payment history + receipt download (§4 TENANT).

## Acceptance Criteria
1. Scenario B: rent 25,000, pays 15,000 → payment 15,000 recorded, allocated to invoice, invoice
   PARTIALLY_PAID, due 10,000, receipt 15,000 (§143 B).
2. Scenario C: rent 20,000, pays 50,000 → 20,000 allocated, invoice PAID, advance ledger 30,000, receipt
   50,000 (§143 C).
3. §22 example: Jan 20,000 + Feb 20,000, pays 30,000 → Jan 20,000, Feb 10,000, Feb remaining 10,000.
4. UI flow follows §126 steps 1–8 and C-5 (invoice selection, allocation toggle, summary); no platform fee
   or payout split appears anywhere (C-4); no gateway "pay" action (C-9).
5. All STORY-002 invariants, idempotency, audit and outbox criteria apply unchanged (§113, §16, §114, §126).
6. Projection consumer is idempotent: replaying the same `rent.invoice.created` creates no duplicate
   projection; an allocation against an invoice not yet projected is rejected (404), never guessed.
7. Authorization: rent collection permission per Q-BOS-004-03 (§4 PROPERTY_MANAGER/RENT_MANAGER
   "Rent collection", "Payment recording", "Receipt generation"); entitlement per Q-BOS-004-21; tenant sees
   only own payments (§4 TENANT); cross-building 404.
8. Reversal (STORY-004) of a rent payment emits `payment.reversed` and rental-visible invoice state returns
   to the pre-payment status (§113.5).

## Tasks
Proposed lanes only — no TASK files yet.
- BE: rent-invoice projection consumer, RENT payment path, events contract with BOS-003 — L
- Mobile: collect rent (mobile/25), tenant payments tab — M
- QA: Scenarios B/C, §22 example, consumer idempotency, cross-service contract test — M
- Size: **L** (uncertainty high: depends on BOS-003 contracts not yet written).

## Dependencies
STORY-002, STORY-003 (STORY-004/005 for reversal/advance reuse). **External: BOS-003** (tenants, leases,
rent invoices, `rent.invoice.created` contract). Q-BOS-004-03, -11, -21, -22.

## Out of Scope
Rent invoice generation, late fees, adjustments (BOS-003 §21, §61–62); security deposit collection
(§20 "recorded separately" — follow-up); rent-due reminders (BOS-007); "Fund Allocation Split",
receipt-screenshot upload as mandatory (mobile/25 UI-only).

## References
BRD §4, §8.3, §8.4, §14, §21–23, §63, §92, §108, §113, §126, §143 B/C, §146; UI-INDEX mobile/25, C-4,
C-5, C-9; gaps/mobile-rental-finance.md §24–25; BACKLOG.md BOS-004 → BOS-003 dependency.
