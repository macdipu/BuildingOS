# STORY-005: Work deadlines and overdue detection

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M (scheduler + event; small UI).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Work orders past their due date are detected and announced as events so BOS-007 can notify and
BOS-008 can show them; staff see overdue work in the register.

## Scope
- Scheduled job "Work overdue detection" (§112), single-execution or idempotent across replicas.
- Event `work.overdue` (§8.5); due-soon event at 3 days before (§115 Work Deadline) if
  Q-BOS-006-20 approves its name.
- List filter/badge "Overdue" on Work Order List; due-date filter already in STORY-002.

## Acceptance Criteria
1. A work order is overdue when `due_date` has passed (Asia/Dhaka, §109) and status is not
   COMPLETED/CANCELLED (§27, §8.5).
2. The job emits exactly one `work.overdue` per work order per overdue episode, even with several
   service instances or reruns (§112 "single-execution or safely idempotent"); event via outbox (§15).
3. Changing `due_date` to a future date ends the overdue episode; a later pass may emit again.
4. Due-soon event at 3 days before due (§115) — only if Q-BOS-006-20 is approved; otherwise not
   built.
5. Work Order List shows an overdue indicator and supports filtering to overdue items (§72 due date
   filter).
6. Job run outcome (count, failures) is logged with correlation id; no personal data in logs.

## Tasks
- BE — scheduled job with lock/idempotency marker, overdue query, outbox event (M)
- Mobile — overdue badge + filter chip (S)
- QA — multi-instance/rerun idempotency test, timezone boundary test (S)

## Dependencies
STORY-002 (STORY-003 for terminal statuses). Q-BOS-006-20.

## Out of Scope
Push/inbox delivery and recipient choice (BOS-007, §115), calendar (§88, BOS-008), SLA timers
(UI-only, gaps 35/37).

## References
BRD §8.5, §15, §27, §72, §109, §112, §115.
