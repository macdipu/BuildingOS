# STORY-004: Notice, termination, move-out, expiry and renewal

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
An operator can end a tenancy through the BRD lease states — give notice, terminate, record
move-out, cancel a never-activated lease, renew — and leases past their end date expire, so
units become vacant correctly and history is preserved.

## Scope
- Use cases: give notice (`POST /api/v1/leases/{id}/notice`, §18), terminate, record
  move-out (`POST /api/v1/leases/{id}/move-out`, §18, `MoveOutTenantUseCase` §12), cancel
  draft, renew; `Notice`, `MoveOut` (and `MoveIn` if Q-BOS-003-18 keeps it) entities (§8.3).
- State transitions over §20 states: `ACTIVE → NOTICE_GIVEN`, `ACTIVE|NOTICE_GIVEN →
  TERMINATED`, `ACTIVE|NOTICE_GIVEN → EXPIRED`, `DRAFT|PENDING → CANCELLED` (Q-BOS-003-20,
  -21).
- Scheduled lease-expiry job, single-execution or idempotent (§112).
- Events via outbox: `tenant.notice.submitted`, `lease.terminated`, `unit.vacated` (§8.3).
- Flutter: Lease Detail `Give Notice`, `Terminate`, `Renew` (§60); Tenant Detail `Give
  Notice`, `Move Out` (§57); Tenant List / Lease List "Notice", "Moved out", "Expired" filters
  populated (§55, §58).

## Acceptance Criteria
1. Give Notice on an `ACTIVE` lease records the notice (fields per Q-BOS-003-18) and moves
   the lease to `NOTICE_GIVEN`; `tenant.notice.submitted` is published via outbox.
2. Terminate on an `ACTIVE`/`NOTICE_GIVEN` lease moves it to `TERMINATED`, requires a reason,
   and is audited (§114 "Lease termination"); `lease.terminated` is published via outbox.
3. Move Out records the move-out and closes the tenancy (§20 "Move-out closes tenancy"); the
   unit has no active primary lease afterwards and `unit.vacated` is published (§8.3). A new
   lease for that unit can then be activated (STORY-003 AC2).
4. Cancel applies only to never-activated leases and produces `CANCELLED` (Q-BOS-003-21).
5. The expiry job moves leases past their end date to `EXPIRED` per Q-BOS-003-20; running
   it twice, or on two instances at once, produces one transition and one event per lease
   (§112).
6. Renew follows Q-BOS-003-19 and never edits the ended lease's terms; the previous lease
   stays readable with its history (§20 immutability).
7. Every transition stores actor + timestamp + from/to state; invalid transitions are
   rejected with a stable error code; concurrent transitions on one lease return
   `VERSION_CONFLICT` (§129).
8. Ended leases (`EXPIRED`, `TERMINATED`, `CANCELLED`) are never deleted (§119 "Historical
   entities should normally transition status").
9. Lease end does not create or reverse any payment or deposit-refund record (BOS-004).
10. Only write roles (Q-BOS-003-04) may perform these actions; permission `lease.terminate`
    (§137).

## Tasks
Proposed task lanes:
- BE/DB: notice / move_out tables, transition use cases, state-machine guard — M
- BE: expiry scheduled job with lock/idempotent claim, events + contracts (3) — M
- Mobile: Give Notice / Terminate / Move Out / Renew flows, filter wiring — M
- QA: transition matrix tests, job double-run test, `unit.vacated` outbox test — S

Size: **L** (uncertainty high: notice/move-out/renew/expiry semantics are BRD-open).

## Dependencies
- STORY-003. Can run in parallel with STORY-005.
- Questions: Q-BOS-003-18, -19, -20, -21.

## Out of Scope
- Lease-expiry reminders at 60/30/7 days (§115) → BOS-007; this story only exposes end dates
  and the expiring filter.
- Security-deposit settlement at move-out (BOS-004).
- Move-out inspection checklists (mockup only).

## References
BRD §8.3, §12, §18, §20, §55, §57, §58, §60, §112, §114, §115, §119, §129, §137;
gaps/mobile-rental-finance.md 19, 20, 22; gaps/mobile-core.md 07.
