# STORY-007: Meeting conduct — minutes, decisions, files, close

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-007

## Objective
Record what happened at a meeting — minutes, decisions with owner/deadline/status, and files — so the
building "maintains meeting decisions" (§146 #20).

## Scope
- Use cases: StartMeeting, AddMinutes, AddDecision, UpdateDecisionStatus, AttachMeetingFile, CloseMeeting; `meeting.decision.recorded` event (§8.6).
- `Create Work from Decision` (§29 "optionally create work orders", §82) — only when BOS-006 work-service exposes a create-work API (Q-BOS-007-16).
- Flutter Meeting Detail tabs Minutes / Decisions / Files and buttons Start Meeting, Add Decision, Add Minutes, Create Work from Decision, Close Meeting (§82).

## Acceptance Criteria
1. Decision fields: decision_text, owner, deadline, status (§29); status values per Q-BOS-007-14.
2. Adding a decision emits `meeting.decision.recorded` via outbox (§8.6, §15).
3. Start/Close follow the lifecycle decided in Q-BOS-007-14; whether minutes/decisions are editable after Close is part of Q-BOS-007-14.
4. Files reuse the STORY-003 attachment port and §131 limits; access limited to meeting viewers (§131, §110).
5. `Create Work from Decision` calls work-service and stores the returned work id on the decision; it is hidden/disabled until BOS-006 provides the API (§29, Q-BOS-007-16).
6. Start, minutes, decisions, file changes and close are audited (§114; backlog audit rule).
7. Decisions count on Meetings List (§80) reflects recorded decisions.

## Tasks
- BE/DB: conduct use cases, decision entity/event, work-service client (behind port) — M
- Mobile: detail tabs Minutes/Decisions/Files + actions — M
- QA: lifecycle, audit, attachment authz, work link (contract stub until BOS-006) — S

## Dependencies
STORY-006, STORY-003; BOS-006 for AC 5; Q-BOS-007-14, -16.

## Out of Scope
Voting/quorum (C-13); decision follow-up reminders (no BRD rule).

## References
BRD §8.6, §15, §29, §80, §82, §110, §114, §131, §146 #20.
