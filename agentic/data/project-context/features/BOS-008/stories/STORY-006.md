# STORY-006: Main dashboard common cards and permission-driven actions

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Building admins (and any role routed to the main dashboard per Q-BOS008-06) see the §43 common cards
for the whole building and permitted quick actions.

## Upstream event sources
- rental-service (BOS-003): `rent.invoice.created`, `rent.invoice.overdue`, `lease.activated`, `lease.terminated`, `unit.vacated`
- finance-service (BOS-004): `payment.allocated`, `payment.reversed`, `maintenance.invoice.created`
- work-service (BOS-006): `work.created`, `work.completed` (open maintenance works)
- communication-service (BOS-007): `meeting.created`, `meeting.updated`, `announcement.published`, `announcement.expired`
- building-service: `unit.created`, `unit.updated`

## Scope
- Projection `building_activity_view` (upcoming meeting, latest announcement — metadata only, §150.8)
  plus building-level aggregates reused from STORY-002/003/005 handlers.
- Main dashboard API shape.
- Flutter main dashboard (mockup `mobile/05-main-dashboard`): cards Current dues, Collected this month,
  Outstanding amount, Occupied/vacant, Open maintenance works, Upcoming meeting, Latest announcement;
  buttons Collect Rent, Add Expense, Create Work, Post Announcement, View Report (§43).

## Acceptance Criteria
1. Card and button visibility depends on the caller's permissions in the active building (§43; §4 BUILDING_ADMIN "Access all building data").
2. Each card shows data only once its upstream source module is live; a card whose source is not deployed is hidden (Q-BOS008-12 default).
3. Latest announcement shows only announcements visible to the caller (§138 `announcement.read`), EMERGENCY prioritized (§115, §150.2 categories per C-12).
4. Occupied/vacant reflects `lease.activated` / `unit.vacated` (§8.3).
5. View Report opens the STORY-007 Reports screen; hidden without `report.read` (§137).
6. Mockup-only "System Mode" banner is not built; header shows the user's actual BRD role, not "Building Lead" (gaps/mobile-core.md 05).
7. Idempotent consumers (§16); tenant isolation by `building_id` (§110).

## Tasks
Proposed lanes:
- BE/DB: building activity projection, main dashboard query composing existing projections — M
- Mobile: main dashboard screen — M
- QA: permission matrix per role, card hide-when-source-absent — S
Size: **M** (uncertainty: medium — aggregates mostly reused).

## Dependencies
STORY-001; benefits from STORY-002/003/005 handlers; BOS-003/004/006/007 per card. Q-BOS008-06, Q-BOS008-12.

## Out of Scope
Calendar (§88), notifications inbox (§85, BOS-007).

## References
BRD §4, §43, §115, §137–138, §150.2, §150.8; UI `mobile/05-main-dashboard`; gaps/mobile-core.md §05; UI-INDEX C-12.
