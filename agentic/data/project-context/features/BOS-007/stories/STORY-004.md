# STORY-004: Notification service, device registry and notice push (EMERGENCY bypass)

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-007

## Objective
Published notices reach every audience member's active devices as FCM push and as a persistent inbox
entry; EMERGENCY notices always arrive (Scenario J), and tapping a push opens the right building
(Scenario N for notices).

## Scope
- notification-service skeleton (own DB, Flyway, outbox for `notification.delivered|failed`, Kafka consumer with dedup) — owner/deployable per Q-BOS-007-01/-02.
- Device registry: `POST /api/v1/me/devices`, `DELETE /api/v1/me/devices/{deviceId}` (§150.7).
- Consumer for `announcement.published` → fan-out to audience → `Notification` (inbox row) + `NotificationDelivery` per device → FCM adapter behind a port; retry with backoff + DLQ (§96, §150.4).
- Preference check reads `NotificationPreference` with §150.4 defaults (management UI is STORY-005).
- Flutter: register/refresh token after login and on rotation, revoke on logout (extend existing `core/services/push_notification/firebase_notification_service.dart`); push tap → deep link → building switch after membership re-check.

## Acceptance Criteria
1. After login the app registers its FCM token with platform ANDROID|IOS|WEB, app_version, locale; token rotation updates the same registration; logout revokes it (§150.4).
2. A user may have several devices; a notification fans out to all non-revoked devices (§150.4).
3. Tokens FCM reports as invalid are revoked automatically (§150.4).
4. `announcement.published` with priority EMERGENCY is pushed to every resolved audience member regardless of preference and quiet hours, except platform limitations (§28, §115, §150.4).
5. Non-EMERGENCY notices respect the `notices` preference (ALL | IMPORTANT_ONLY; mapping of priorities to "important" per Q-BOS-007-07) (§150.4).
6. Push payload contains only title, short body, `building_id`, destination deep link and notification id — no NID, phone numbers or data beyond the visible text (§150.4, §94).
7. Every notice push also creates an inbox entry, even if push fails or is dismissed (§150.4).
8. Each delivery attempt/failure/retry is recorded in `NotificationDelivery`; retries use backoff; exhausted messages go to DLQ (§96, §150.4); `notification.delivered` / `notification.failed` are emitted via outbox (§150.8).
9. Consumer is idempotent: re-delivery of the same event id creates no duplicate inbox entry or push (§96; backlog "consumer idempotency ships with each event-producing feature").
10. When the recipient has several buildings, the push shows the building name; tapping it switches `activeBuildingId` after membership re-check and opens the notice; if membership was lost, the app shows an access message and does not open it (§150.4, §149.13, Scenario N).
11. A scheduled notice is pushed only when it publishes (§150.2).
12. Devices endpoints act only on the caller's own registrations (`/me`), 404 for others' ids.
13. Push dispatch latency is measured against P95 < 10 s after the event (§150.12) in a non-local environment (evidence task, no local claim).

## Tasks
- BE/DB: notification-service skeleton, device registry use cases, consumer + fan-out, FCM port/adapter, delivery tracking, retry/DLQ — L
- BE: audience recipient lookup (from event snapshot or communication-service internal API — technical design) — S
- Mobile: token lifecycle, push tap routing + building switch — M
- Web: none (optional feature key in plan editor per Q-BOS-007-18) — S
- QA: idempotency, EMERGENCY bypass, invalid-token revoke, payload content assertions, multi-building tap — M

## Dependencies
STORY-001 (event + audience snapshot); Q-BOS-007-01, -02, -05, -07, -18; Firebase project config for each platform (operational).

## Out of Scope
Inbox screen and preference UI (STORY-005); meeting push (STORY-006); chat push (STORY-008); domain reminder consumers (Q-BOS-007-20).

## References
BRD §8.1 Device tokens, §8.7, §28, §94, §96, §115, §149.13, §150.4, §150.7, §150.8, §150.12, §150.13 J/N.
