# STORY-003: Lease activation and rent schedule

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
An operator can activate a lease, which fixes its rent schedule, marks the unit occupied
for rental purposes, and tells other services the lease is active — never allowing two
active primary leases on one unit.

## Scope
- `ActivateLeaseUseCase` (§12) behind `POST /api/v1/leases/{leaseId}/activate` (§18) with
  `Idempotency-Key` (§16 "Lease activation").
- `RentSchedule` created at activation from the lease terms: monthly rent, parking charge,
  other recurring charges, due day, late-fee policy (§59), versioned or tied to the lease
  period (§20; Q-BOS-003-16).
- `Save & Activate` on Create Lease and `Activate` on Lease Detail (§59, §60).
- Event `lease.activated` v1 via outbox (§8.3, §17), payload proposal in Q-BOS-003-17.
- Unit occupancy for rental reads (Occupied/Vacant, §50 header) derived from active leases in
  rental-service.
- Flutter: Lease Detail (mobile/22) sections Lease summary, Tenant, Unit, Rent schedule,
  Deposit, Advance, Documents (§60); Invoice history section empty until STORY-005.

## Acceptance Criteria
1. Activating a `DRAFT` lease moves it to `ACTIVE` (or `PENDING` if Q-BOS-003-15 adopts it)
   and records actor + timestamp; activation of any other state is rejected.
2. If the unit already has an `ACTIVE` or `NOTICE_GIVEN` primary lease, activation fails with
   `ACTIVE_LEASE_EXISTS` (§20 "zero or one active primary lease", §128). Two concurrent
   activations for the same unit result in exactly one success (unit-level lock or unique
   constraint; §129).
3. A repeated request with the same `Idempotency-Key` and body returns the original result
   and creates no second schedule/event; the same key with a different body returns
   `DUPLICATE_IDEMPOTENCY_KEY` (§16, §128).
4. The rent schedule stores amounts with explicit currency BDT (§113 #9); after activation
   the lease's financial terms are immutable except through the approved correction/renewal
   path (§20 "Historical leases remain immutable except safe corrections"; Q-BOS-003-16).
5. Activation writes an audit record (§114 "Lease activation") and `lease.activated` in the
   same transaction (§15); the event contains identifiers and the terms finance needs, never
   NID or contact data.
6. Lease Detail shows §60 sections; buttons are state-dependent: `Activate` and `Edit Draft`
   only for `DRAFT`; `Give Notice`, `Terminate`, `Renew` from STORY-004; `Download Agreement`
   when a document exists (§60).
7. Mockup-only "Legal Checkpoints" (stamp duty, DMP verification, e-signatures) are not shown
   (C-6, gap file 22).
8. Only write roles (Q-BOS-003-04) may activate; permission name `lease.activate` (§137).

## Tasks
Proposed task lanes:
- BE/DB: rent_schedule table, activate use case with unit lock + idempotency store,
  occupancy query — M
- BE: `lease.activated` contract + outbox — S
- Mobile: Save & Activate, Lease Detail page — M
- QA: concurrency test (parallel activation), idempotency replay test, audit/outbox
  atomicity test — S

Size: **L** (uncertainty: Q-BOS-003-15/16/17 change state machine and payload).

## Dependencies
- STORY-002.
- Questions: Q-BOS-003-14, -15, -16, -17, -25 (late-fee field shape).

## Out of Scope
- Generating the first invoice (STORY-005 decides whether activation triggers it,
  Q-BOS-003-22).
- Deposit / advance-rent collection and advance ledger (BOS-004).
- Move-in checklist/inspection (mockup 07 only; not in BRD screen specs).

## References
BRD §8.3, §12, §15, §16, §17, §18, §20, §50, §59, §60, §113, §114, §128, §129, §137;
gaps/mobile-rental-finance.md 21, 22.
