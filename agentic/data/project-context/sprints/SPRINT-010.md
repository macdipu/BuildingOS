# SPRINT-010: Notification delivery and work-cost linkage

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Notices have a full lifecycle and push delivery with an inbox; work costs link to expenses.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-007-S02 | [BOS-007 STORY-002](../features/BOS-007/stories/STORY-002.md) | L | BE, Mobile, QA | Q-BOS-007-06, -08, -09, -12, -13, -23 |
| BOS-007-S04 | [BOS-007 STORY-004](../features/BOS-007/stories/STORY-004.md) | L | BE/Integration, Mobile, DevOps, QA | Q-BOS-007-01 (new notification-service), -02, -05, -07, -18 |
| BOS-007-S05 | [BOS-007 STORY-005](../features/BOS-007/stories/STORY-005.md) | M | BE, Mobile, QA | Q-BOS-007-17, -19 |
| BOS-006-S08 | [BOS-006 STORY-008](../features/BOS-006/stories/STORY-008.md) | M | BE/Integration, QA | Q-BOS-006-12, -13, -14 |

## Order and Dependencies
BOS-007 S02 and S04 after S01; S05 after S04. BOS-006 S08 after BOS-006 S03 + BOS-004 S07.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 10 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
EMERGENCY notice push, inbox, preferences; work cost appears on the expense.

## Decision gates before or during this sprint
- FCM projects per platform (BOS-007 Q-02)

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
