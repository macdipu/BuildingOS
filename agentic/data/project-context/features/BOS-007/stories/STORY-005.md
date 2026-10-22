# STORY-005: Notification inbox and per-building preferences

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-007

## Objective
Users see every delivered notification in an in-app inbox (§85) and control non-critical
notifications per building (§115, §150.4).

## Scope
- APIs: `GET /api/v1/me/notifications?buildingId=&category=&cursor=`, `POST /api/v1/me/notifications/{id}/read`, `POST /api/v1/me/notifications/read-all`, `GET /api/v1/me/notification-preferences?buildingId=`, `PUT /api/v1/me/notification-preferences` (§150.7).
- Flutter: new feature `notifications` — Notifications screen (tabs All/Finance/Building/Work/Announcement, Mark read, Mark all read, Open destination) (§85); Notification Settings under Settings → Notification (§89, §150.9); unread badge.

## Acceptance Criteria
1. Inbox lists only the caller's notifications, filterable by building and category, cursor-paginated (§118, §150.7).
2. Tabs map notification categories to All/Finance/Building/Work/Announcement per Q-BOS-007-19 (§85).
3. Mark read / Mark all read are idempotent and scoped to the caller (and to the building when filtered) (§85).
4. Open destination deep-links and switches active building after membership re-check (§85, §150.4).
5. Chat messages never appear in the inbox (§85, §150.4).
6. Preferences are per user per building with keys and values exactly: notices ALL|IMPORTANT_ONLY, chat_community ALL|MENTIONS_ONLY|OFF, chat_direct ALL|OFF, finance ON|OFF, work ON|OFF, meetings ON|OFF; chat keys hidden until chat ships; quiet_hours per Q-BOS-007-17 (§150.4).
7. Any user may update only their own preferences (`notification.preference.update`, §150.10).
8. The settings screen states that EMERGENCY notices always arrive (§115, §150.4).
9. Preference changes apply to the next dispatch (STORY-004 consumer reads current preference).

## Tasks
- BE: inbox query/read use cases, preference get/put — M
- Mobile: notifications feature (inbox + settings + badge) — M
- QA: scoping, pagination, preference effect on dispatch — S

## Dependencies
STORY-004; Q-BOS-007-17, -19.

## Out of Scope
Reminder timing configuration (§89 "Reminder timing" belongs with domain reminders, Q-BOS-007-20); email/SMS channels (§8.7 future).

## References
BRD §85, §89, §115, §118, §150.4, §150.7, §150.9, §150.10.
