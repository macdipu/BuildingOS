# STORY-005: Scheduled monthly rent invoice generation

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
Every active lease gets exactly one rent invoice per billing period, issued automatically by
a scheduled job and on demand from `Generate Invoices`, so tenants and operators know what
is due.

## Scope
- `GenerateRentInvoiceUseCase` (§12) invoked by (a) a monthly scheduled job (§112 "Monthly
  rent invoice generation") and (b) `Generate Invoices` on Rent Overview (§61) for the
  active building and a chosen billing period, with `Idempotency-Key` (§16 "Generate
  invoice").
- `RentInvoice` with §21 lines: base rent, parking charge, service charge, other recurring
  charge, adjustments, late fee, discount, previous balance reference; status `ISSUED` on
  generation (§21 statuses; `DRAFT` usage per Q-BOS-003-22).
- Invoice number generated server-side (format Q-BOS-003-23).
- Event `rent.invoice.created` v1 via outbox (§8.3); consumed later by finance (§8.4).
- Invoice read API minimal: `GET /api/v1/rent-invoices` (§18) for verification; full screens
  in STORY-006.

## Acceptance Criteria
1. For a billing period, the job creates one invoice per lease that is billable in that
   period (states and dates per Q-BOS-003-20/22) and none for draft, cancelled, or ended
   leases outside the period.
2. Uniqueness: at most one non-void invoice per (lease, billing period), enforced by a
   database constraint; re-running the job or the manual action for the same period creates
   nothing new and reports the existing invoices (§112 "single-execution or safely
   idempotent", §16).
3. Running the job concurrently on two instances yields the same result as one run (§112).
4. Invoice amounts are computed from the lease's rent schedule in effect for the period
   (§20 "Rent amount is versioned or tied to lease period"), currency BDT (§113 #9), billing
   period and due date in Asia/Dhaka (§109).
5. `due = invoice total − allocated payment` (§21); with no allocations yet (BOS-004) the
   outstanding equals the total. Due is never computed from raw payment totals (§21, §144).
6. Invoice numbers are unique and server-generated (Q-BOS-003-23).
7. Each invoice and its `rent.invoice.created` outbox row are written in one transaction
   (§15); a failure of one lease does not roll back or duplicate others, and is reported/
   retried.
8. Manual generation requires `rent.invoice.generate` (§137) via a write role
   (Q-BOS-003-04); the run is audited (actor, period, counts) — mirrors §114 "Maintenance
   fee generation".
9. Invoices of building A are never generated from or visible to building B (tenant
   isolation).
10. The job's schedule and timezone are configuration, not code constants (D-27 spirit,
    §109).

## Tasks
Proposed task lanes:
- BE/DB: rent_invoice + rent_invoice_line tables, unique (lease, period) constraint, number
  sequence per building/period — M
- BE: generation use case, scheduler with distributed lock or idempotent claim, manual
  endpoint with idempotency — M
- BE: `rent.invoice.created` contract + outbox — S
- QA: double-run / concurrent-run tests, period boundary tests (Asia/Dhaka), activation
  mid-month cases per Q-BOS-003-22 — M

Size: **L** (uncertainty high: Q-BOS-003-02 and -22 can change ownership and amounts).

## Dependencies
- STORY-003 (active leases + rent schedule). STORY-004 for expiry interplay (can follow).
- Questions: **Q-BOS-003-02 (blocking)**, -20, -22, -23, -24.

## Out of Scope
- Invoice screens (STORY-006), adjustments/void/overdue (STORY-007).
- Payment allocation and PAID/PARTIALLY_PAID transitions (BOS-004, Q-BOS-003-03).
- Rent reminders (§115) → BOS-007.
- Maintenance invoices (§25) → BOS-004.

## References
BRD §8.3, §8.4, §12, §15, §16, §17, §18, §20, §21, §61, §108, §109, §112, §113, §114,
§137, §144; BACKLOG Q-03; D-27.
