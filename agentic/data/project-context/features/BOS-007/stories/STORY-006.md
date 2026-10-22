# STORY-006: Meetings — create, invite, list and detail

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-007

## Objective
Authorized staff schedule a building meeting with agenda and participants and publish the invitation;
participants are notified and can view upcoming/past meetings.

## Scope
- communication-service `meeting` feature: CreateMeeting (Save), PublishInvitation, CancelMeeting, UpdateMeeting, ListMeetings, GetMeeting; entities Meeting, AgendaItem, MeetingParticipant (§8.6, §29).
- Events `meeting.created` (on Publish Invitation) and `meeting.updated` via outbox (§8.6, Q-BOS-007-15); notification-service consumes `meeting.created` → push + inbox to participants (§150.4).
- Flutter feature `meetings`: Meetings List (§80), Create Meeting (§81), Meeting Detail tabs Overview/Agenda/Participants (§82).

## Acceptance Criteria
1. Meeting fields: title, meeting_type, scheduled_at, location (Venue), agenda items, participants (§29, §81); meeting_type values, status lifecycle and creator permission per Q-BOS-007-14.
2. `Save` stores without notifying; `Publish Invitation` emits `meeting.created` once; `Cancel` follows Q-BOS-007-14 (§81).
3. Participants are current building members; only participants and holders of the creator permission can view a meeting (§110, Q-BOS-007-14).
4. Participants receive push (respecting `meetings` preference ON|OFF) and an inbox entry on `meeting.created` (§150.4); push on updates per Q-BOS-007-15.
5. Meetings List rows show title, date, type, status, decisions count; `+ Create Meeting` only for permitted roles; Filter available (§80). Calendar button out of scope (§88 not in this epic).
6. Create/update/publish/cancel are audited (backlog: audit alongside privileged/domain writes; §114).
7. All queries filtered by building and membership (§110); outbox atomic with the write (§15); consumer idempotent (§96).

## Tasks
- BE/DB: meeting aggregate + use cases, events, contracts — M
- BE: notification consumer for `meeting.created` — S
- Mobile: meetings list/create/detail (3 tabs) — M
- QA: lifecycle, participant visibility, push/preference — S

## Dependencies
STORY-001 (service foundation); STORY-004 (push AC 4); Q-BOS-007-14, -15.

## Out of Scope
Minutes, decisions, files, Start/Close (STORY-007); Calendar (§88); AGM quorum/RSVP/voting (C-13).

## References
BRD §8.6, §15, §29, §80–82, §96, §110, §114, §150.4; UI-INDEX "No mockup" §80–82; C-13.
