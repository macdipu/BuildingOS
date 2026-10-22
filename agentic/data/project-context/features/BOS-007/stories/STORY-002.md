# STORY-002: Notice lifecycle — schedule, expire, pin, edit history, delete, acknowledge, targeted audiences

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-007

## Objective
Complete the notice board rules of §28/§150.2 so publishers can manage the full lifecycle and members
can acknowledge mandatory notices.

## Scope
- Use cases: SchedulePublish (publish_at), ExpireAnnouncement (auto at expires_at + `Expire Now`), PinAnnouncement, UnpinAnnouncement, EditAnnouncement (versioned), DeleteAnnouncement, AcknowledgeAnnouncement.
- Audiences SPECIFIC_UNITS and SPECIFIC_USERS (§28) resolved per Q-BOS-007-23.
- Scheduler for publish_at / expires_at inside communication-service.
- Flutter: Active/Scheduled/Expired tabs and Filter (§77), Preview (§78), Publish now/schedule + Expiry + Require acknowledgement + Specific units/users pickers (§78), Acknowledge / Edit if allowed / Expire Now / Share (§79), Pinned badge and "edited" marker.

## Acceptance Criteria
1. A notice with future `publish_at` is in SCHEDULED, is not visible to the audience and not pushed until published; at publish time the audience is resolved and `announcement.published` is emitted once (§150.2, §15).
2. At `expires_at` or on `Expire Now` the notice leaves the active board, stays readable under Expired, and emits `announcement.expired` (§150.2, §79, §150.8).
3. Pin/unpin requires `announcement.pin` (§150.10); whether EMERGENCY auto-pins follows Q-BOS-007-13; pinned notices sort first until unpinned or expired; pin emits `announcement.pinned` (§150.2, §150.8).
4. Editing a published notice stores the prior version; readers see an "edited" marker; emits `announcement.updated`; editable fields/roles per Q-BOS-007-08 (§150.2, §79 "Edit if allowed").
5. Delete semantics per Q-BOS-007-09; the deleted notice disappears from the board for readers.
6. Publish, edit, expire, pin/unpin and delete are each audited with actor, reason where given, before/after for edits (§150.2, §114).
7. When `requires_acknowledgement` is true, members in the audience see `Acknowledge`; acknowledging is per user, idempotent, and the publisher sees acknowledged vs read counts (§28, §79, §150.2).
8. Members who join after publish see notices still active for their audience (§150.2); their effect on Read % follows Q-BOS-007-06.
9. All state transitions reject invalid moves (e.g. pin an expired notice, edit a deleted notice) with §128 errors; concurrent edits are rejected via optimistic `version`.
10. Share behaviour per Q-BOS-007-12; no public unauthenticated link.
11. Tenant isolation and membership filtering as STORY-001 AC 7 for every new endpoint (§110).

## Tasks
- BE/DB: lifecycle use cases, version table, scheduler, targeted-audience resolution, events — L
- Mobile: tabs/filter, create-form extensions, detail actions, edited/pinned markers — M
- QA: state-machine tests, scheduler timing tests, audit assertions, audience edge cases — M

## Dependencies
STORY-001; Q-BOS-007-06, -08, -09, -12, -13, -23. SPECIFIC_UNITS resolution of tenants needs BOS-003.

## Out of Scope
Attachments (STORY-003), push for scheduled publish (STORY-004 consumes the same event), comments.

## References
BRD §28, §77–79, §114, §128, §150.2, §150.7, §150.8, §150.10; gaps/mobile-ops-community.md (39 BRD-only tabs, 40 BRD-only Edit/Expire Now).
