# STORY-007: Asset maintenance work and service-due reminders

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M (uncertainty medium:
Q-BOS-006-08/09 decide the rules).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Link maintenance work to assets, show each asset's work history, and emit service-due reminders.

## Scope
- Optional `asset_id` on work orders (Q-BOS-006-09).
- Asset Detail `Create Maintenance Work` (§84) → Create Work Order pre-filled with asset.
- Asset "Work history" (§84) = work orders linked to the asset.
- `next_service_date` handling and scheduled "Asset maintenance reminder" job (§112) per
  Q-BOS-006-08; service-due event on `work.events`.

## Acceptance Criteria
1. `Create Maintenance Work` opens the STORY-002 form with the asset pre-linked; the asset must be
   in the same building and not RETIRED (§84, §110).
2. Asset Detail shows Work history (linked work orders, newest first) and Next service (§84).
3. On completion of a linked work order, `last_service_date` / `next_service_date` update only as
   approved in Q-BOS-006-08/09; otherwise they stay manual fields.
4. The reminder job (§112) emits one service-due event per asset per due cycle, idempotent across
   instances/reruns, via outbox (§15); lead time per Q-BOS-006-08 (no BRD value).
5. Work Order List can filter by asset (extension of §72 filters, needs operator OK as part of
   Q-BOS-006-09).

## Tasks
- BE/DB — asset_id on work_order, history query, next-service rule, reminder job + event (M)
- Mobile — Create Maintenance Work entry, work history list on asset detail (S)
- QA — job idempotency, cross-building link rejection, date rule tests (S)

## Dependencies
STORY-002, STORY-003 (completion), STORY-006. Q-BOS-006-08, -09.

## Out of Scope
Reminder delivery (BOS-007), calendar "Asset service" (§88, BOS-008), predictive maintenance
(§3.2), advanced asset maintenance (§125).

## References
BRD §15, §30, §72, §84, §88, §110, §112, §125.
