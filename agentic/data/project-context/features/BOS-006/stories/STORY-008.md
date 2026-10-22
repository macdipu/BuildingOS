# STORY-008: Expense linkage to BOS-004 (work cost, contractor payments)

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M (uncertainty high: depends on
unplanned BOS-004 contracts, Q-BOS-006-12/13/14).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
From a work order, staff record a related expense in BOS-004, and the work order and its contractor
show the linked cost and payments.

## Scope
- Work Order Detail `Add Expense` (§74) opens BOS-004 Add Expense (§70, mobile/32) pre-filled with
  `work_order_id` (§26 "work_order_id optional", §70 "Work order optional") and vendor = contractor
  name.
- finance-service validates `work_order_id` belongs to the same building (HTTP call to work-service
  or event-fed projection — decided in BOS-004 technical design; no cross-DB reads, docs rule 6).
- work-service consumes `finance.events` (`expense.created`, `expense.approved` §8.4 + paid/rejected/
  void per Q-BOS-006-13) into `work_expense_link`; idempotent consumer.
- Work Order Detail "Cost": estimated vs actual (Q-BOS-006-12); `actual_cost_reference` (§27).
- Contractor List "Total paid" (§75) and Contractor Detail "Payments" (§76) per Q-BOS-006-14.
- Expense Detail "Related work" (§71) links back to the work order (BOS-004 screen, mobile/33).

## Acceptance Criteria
1. `Add Expense` is visible only to users who also hold `expense.create` (§137); the expense is
   created by finance-service with `Idempotency-Key` (§16 "Create expense").
2. An expense whose `work_order_id` points to another building's or a non-existent work order is
   rejected (§5, §110).
3. Consuming the same finance event twice does not double-count (consumer idempotency, BACKLOG;
   §16 intent).
4. Work order actual cost = rule in Q-BOS-006-12 (default: sum of APPROVED/PAID, excluding
   DRAFT/SUBMITTED/REJECTED/VOID per §26 statuses), with currency (§113.9); each linked expense
   listed with status.
5. Contractor "Total paid" counts only PAID expenses per Q-BOS-006-14 default; reversal/void
   removes it (§26 VOID, §113 append-oriented records — work-service never edits finance data).
6. Expense status labels are the §26 enum (C-8), not mockup "Settled"/"Approved & Paid".
7. Projection lag is shown as eventual (no blocking); if finance-service is down, work order detail
   still renders with last known cost.

## Tasks
- BE — finance event consumer + projection + cost/total-paid queries (M)
- BE (finance-service, BOS-004 lane) — `work_order_id` validation + event payload fields (S, owned
  by BOS-004)
- Mobile — Add Expense entry with prefill; cost section; contractor payments list (S)
- QA — idempotent consumer, isolation, status-mix totals, contract test for finance events (§98) (M)

## Dependencies
STORY-003; BOS-004 expense slice (finance-service, §26/§70/§71, events). Q-BOS-006-12, -13, -14.

## Out of Scope
Expense approval/payment workflow itself (BOS-004), maintenance-bill charging of residents
(mobile 38 "Add to Monthly Maintenance Bill" — needs BOS-004/operator decision; not in §27),
in-app payment (C-9).

## References
BRD §5, §8.4, §16, §26, §27, §70, §71, §74–76, §98, §110, §113, §137; UI-INDEX C-8, C-9;
gaps 38 and mobile-rental-finance (31–33).
