# STORY-008: [DEFERRED] Community chat core — building/audience channels, real time, offline, push

Status: PROPOSED — DEFERRED (planning draft 2026-10-22; not approved). Chat MVP inclusion is OPEN,
default deferred to Phase 6 (§125, §150.3). Do not start until Q-BOS-007-22 is decided.

## Parent Epic
EPIC-BOS-007

## Objective
Members of one building talk in the derived `BUILDING_COMMUNITY` and `AUDIENCE_GROUP` channels in real
time, offline-tolerant, with push to offline members (Scenarios K, L, N).

## Scope
- communication-service `chat` feature: channels (derived types only), members (derived sync), messages (text, emoji, reply-to, @mentions, attachments), edit/delete own, read cursor, mute/notification level.
- Membership sync by consuming membership/role/ownership/lease events (§150.3, §150.8) — source per Q-BOS-007-03.
- WebSocket (STOMP) via gateway with JWT + membership checks; history polling fallback; horizontal fan-out via Redis pub/sub or Kafka (§150.5) — infra per Q-BOS-007-21.
- Notification-service consumer for `chat.message.posted` (collapse per channel, suppress while channel open, mute/preference/block) (§150.4).
- Entitlement gate `community_chat.enabled` / `community_chat.attachments.enabled` → `FEATURE_NOT_ENTITLED` (§150.11), adapted to the per-user model per Q-BOS-007-05.
- Flutter: Community Home Chat segment, Chat List, Conversation, Channel Info (members, mute) (§150.9); local cache + PENDING outbox (§150.6).

## Acceptance Criteria
1. Each channel belongs to exactly one building; no cross-building chat (§150.3, §149.13).
2. Derived channel membership follows building membership and role; users cannot leave derived channels but may mute (§150.3).
3. Losing building membership removes channel access immediately; authored history remains attributed to display name (§150.3, Scenario K).
4. Members display as display name + role + unit label; phone, email, NID never exposed (§150.3, §94, Scenario K).
5. `POST .../messages` is idempotent on `client_message_id` per sender; offline message shows "sending", is delivered once on reconnect (§150.3, §150.6, Scenario L).
6. Edits are marked; deletes leave a tombstone (§150.3).
7. Read state is a per-member last-read cursor (§150.3); typing/presence are ephemeral and never persisted (§150.3).
8. Online members receive new/edited/deleted events over WebSocket; missed events recover via cursor-paginated history (§150.5, §118).
9. Offline members get push per `chat_community` preference, mute and @mention rules; collapsed per channel; suppressed while viewing that channel; not added to inbox (§115, §150.4).
10. Tapping a chat push for another building switches building after membership re-check and opens the channel (Scenario N).
11. Non-entitled callers get `FEATURE_NOT_ENTITLED`; notices and critical push stay available (§150.11).
12. Locked channel / muted member return `CHAT_CHANNEL_LOCKED` / `CHAT_MEMBER_MUTED` (§150.7) (locking itself is STORY-009).
13. Per-user rate limit on send (§104, §150.12); every query filtered by `building_id` and membership (§110, §150.12).
14. Message bodies are never copied into reporting or audit read models (§150.8).
15. NFR evidence: delivery P95 < 1 s, history page P95 < 500 ms (§150.12) in a non-local environment.

## Tasks
- BE/DB: chat aggregate, derived channel sync consumers, message use cases, read cursor, events — XL
- BE: WebSocket endpoint + gateway routing + fan-out infra — L
- BE: chat push consumer in notification-service — M
- Mobile: chat feature (list, conversation, channel info, offline queue) — L
- Web: plan feature keys for chat entitlement (data only, D-27) — S
- QA: Scenarios K/L/N, isolation, idempotency, load evidence — L

## Dependencies
STORY-001, STORY-003 (attachments port), STORY-004, STORY-005; Q-BOS-007-03, -05, -21, -22; BOS-003 for lease-ended sync (§150.8 rental events).

## Out of Scope
DIRECT and CUSTOM_GROUP channels, moderation, reports, blocks (STORY-009); E2E encryption, voice/video, cross-building chat (§3.2, §150.12).

## References
BRD §3.2, §94, §104, §110, §115, §118, §125, §131, §136, §149.13, §150.3–150.13 (Scenarios K, L, N).
