# STORY-009: [DEFERRED] Chat direct and custom groups, moderation, reports and blocks

Status: PROPOSED — DEFERRED (planning draft 2026-10-22; not approved). Blocked by the §150.3 OPEN
decisions (Q-BOS-007-22).

## Parent Epic
EPIC-BOS-007

## Objective
Members start one-to-one and custom group chats where permitted; moderators keep group channels safe;
any member can report or block (Scenario M).

## Scope
- `POST /api/v1/buildings/{buildingId}/chat/channels` for DIRECT / CUSTOM_GROUP; moderation mute/lock; reports list/resolve; blocks add/remove (§150.7).
- Flutter: `+ New Chat`, long-press Report / Delete (moderator), Channel Info moderator actions, Block member, Moderation Reports screen (§150.9).

## Acceptance Criteria
1. DIRECT is between two members of the same building; tenant DIRECT scope and CUSTOM_GROUP creator rule follow Q-BOS-007-22; violations return `CHAT_DIRECT_NOT_ALLOWED` (§150.3, §150.7, §150.10).
2. `chat.moderate` holders may delete any message in group channels of their building, mute a member for a period, and lock a channel (§150.3).
3. Moderators cannot read DIRECT channels they are not part of (§150.3).
4. Any member may report a message; reports go to building moderators; reported content is retained even if the author deletes it (§150.3).
5. A block hides the blocked user's messages from the blocker and prevents DIRECT between them (`CHAT_USER_BLOCKED`) (§150.3, §150.7).
6. Every moderation action (moderator delete, mute, lock, report resolution) is audited with actor, reason and target (§150.3, §114, Scenario M).
7. Platform support staff have no chat access by default; any access follows §149.12 (§150.3).
8. Rate limit on channel creation (§104, §150.12).

## Tasks
- BE/DB: direct/custom channels, moderation, reports, blocks use cases + events — L
- Mobile: new-chat flow, moderation UI, reports screen, block — M
- QA: Scenario M, moderator DIRECT privacy, block semantics, audit — M

## Dependencies
STORY-008; Q-BOS-007-22.

## Out of Scope
Platform-level content moderation tooling in back-office; E2E encryption.

## References
BRD §104, §114, §149.12, §150.3, §150.7, §150.9, §150.10, §150.13 M.
