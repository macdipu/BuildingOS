# STORY-003: Work order lifecycle — assign, start, block, complete

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: L (state machine + three mockup
screens; uncertainty medium, Q-BOS-006-04 drives it).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Staff move a work order through its lifecycle with a visible status timeline, satisfying "track
building work" (§146.19).

## Scope
- Use cases (one per action, §12): AssignWorkOrder, StartWorkOrder, BlockWorkOrder,
  ResumeWorkOrder, CompleteWorkOrder, CancelWorkOrder, UpdateWorkOrder (Q-BOS-006-19).
- WorkAssignment history (§8.5); status transition records for the timeline.
- Events `work.assigned`, `work.started`, `work.completed` (§8.5); `work.blocked`,
  `work.cancelled` only if Q-BOS-006-04 approves them.
- Flutter Work Order Detail (§74) with assign (mobile/36), progress (mobile/37), complete
  (mobile/38) states.

## Acceptance Criteria
1. Transitions follow the approved table (Q-BOS-006-04 default: DRAFT/PLANNED→ASSIGNED→IN_PROGRESS
   ⇄ BLOCKED→COMPLETED; non-terminal→CANCELLED); any other transition → 409/422 with a domain error
   code; COMPLETED and CANCELLED are terminal.
2. `Assign` requires `work.assign` and an active contractor of the same building (§137, §27
   `contractor_id`); `Complete` requires `work.complete` (§137); `Start Work` / `Mark Blocked`
   require `work.assign` unless the operator decides otherwise (BRD silent — Q-BOS-006-04).
3. `Complete` sets `completed_at` (§27) and writes an audit record "Work completion" (§114) with
   actor and timestamp; all other transitions are recorded in the timeline with actor + timestamp.
4. Optimistic locking: a stale `version` on any action → 409 `VERSION_CONFLICT` (§129, §128); the
   app shows a refresh prompt.
5. Each transition and its outbox event commit in one transaction (§15); events carry building id,
   work order id, from/to status, actor, version; consumers dedupe by event id.
6. Detail shows Status timeline, Description, Contractor, Deadlines, Cost (estimated; actual in
   STORY-008), with buttons `Assign`, `Start Work`, `Mark Blocked`, `Complete` shown only when the
   transition is allowed and permitted (§74).
7. Assignee is a contractor only (Q-BOS-006-10); mobile 36 in-house/vendor split, ratings, billing
   classification and cost range are not built (gap 36).
8. Mobile 37 entry-OTP, SKU parts log, SLA widgets not built (gap 37); mobile 38 bKash/Nagad
   "Pay Now" not built (C-9), nor ratings, signature, admin fee, guarantee badge (gap 38).
9. Cross-building action on a work order id → 404 (§5, §110).
10. Contractor List "Active works" reflects ASSIGNED/IN_PROGRESS/BLOCKED counts (§75).

## Tasks
- BE — transition use cases, assignment history, timeline, events, audit, locking (L)
- Mobile — work order detail with timeline and action sheets (assign/start/block/complete) (M)
- QA — transition matrix tests, concurrency (409) test, permission tests, widget tests (M)

## Dependencies
STORY-002. Q-BOS-006-04, -10, -19.

## Out of Scope
Comments/attachments and before/after photos (STORY-004), `Add Expense` (STORY-008), overdue
(STORY-005), notifications to contractors/residents (BOS-007).

## References
BRD §8.5, §12, §15, §27, §74, §75, §114, §128, §129, §137, §146; UI-INDEX C-9; gaps 36–38.
