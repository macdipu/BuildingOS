# STORY-005: Consume advance balance through explicit allocation

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
An authorized user applies an existing advance balance to a newer open invoice through an explicit,
audited allocation, so advance money settles later dues without a new cash payment.

## Scope
- Use case: `AllocateAdvanceUseCase` (§12 `AllocatePaymentUseCase` family); queries for advance balance per
  ledger scope (Q-BOS-004-11).
- API: `GET /api/v1/advance-ledgers?unitId=`, `POST /api/v1/advance-ledgers/{id}/allocations` with
  `Idempotency-Key`.
- Flutter: advance balance shown on the unit/payer context in the maintenance payment flow and an
  "Apply advance" action on an open invoice (no dedicated mockup; plan from BRD text + DESIGN.md).

## Acceptance Criteria
1. Advance is consumed only by an explicit allocation request — never by silently netting at invoice
   generation (§23 "Next invoice can consume advance through an explicit allocation"; Q-BOS-004-12).
2. Allocated amount ≤ available advance balance and ≤ invoice remaining due (§113.2–3).
3. Advance originating from a REVERSED payment is not available (§113.4).
4. The allocation links back to the originating payment(s) so reversal impact is traceable (§22
   "Reconciliation, Reversal").
5. Idempotent (§16); concurrent consumption of the same balance → one succeeds, other 409 (§129).
6. Audit row + `payment.allocated` and `advance.balance.changed` outbox events in the same transaction
   (§8.4, §15, §113.10).
7. Same authorization and tenant isolation rules as STORY-002 (AC 10).

## Tasks
Proposed lanes only — no TASK files yet.
- BE: advance balance query, allocate-advance use case, events — M
- Mobile: balance display + apply-advance action — S
- QA: balance math, reversed-source exclusion, concurrency — S
- Size: **M**.

## Dependencies
STORY-002; interacts with STORY-004 (Q-BOS-004-13). Q-BOS-004-11, -12.

## Out of Scope
Automatic advance consumption; advance refunds/withdrawal (no BRD rule); rent advance (STORY-008 reuses).

## References
BRD §12 (`AllocatePaymentUseCase`), §16, §22, §23, §113, §129; §143 C.
