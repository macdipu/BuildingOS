# SPRINT-011: Meetings and first dashboards

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Meetings with decisions (§146.20) and owner/manager dashboards.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-007-S06 | [BOS-007 STORY-006](../features/BOS-007/stories/STORY-006.md) | L | BE, Mobile, QA | Q-BOS-007-14, -15 |
| BOS-007-S07 | [BOS-007 STORY-007](../features/BOS-007/stories/STORY-007.md) | M | BE, Mobile, QA | Q-BOS-007-14, -16 |
| BOS-008-S02 | [BOS-008 STORY-002](../features/BOS-008/stories/STORY-002.md) | L | BE, Mobile, QA | Q-BOS008-03, -04, -08, -09 |
| BOS-008-S03 | [BOS-008 STORY-003](../features/BOS-008/stories/STORY-003.md) | M | BE, Mobile, QA | Q-BOS008-03, -06, -09 |

## Order and Dependencies
BOS-007 S06 → S07. BOS-008 S02/S03 need BOS-003/004 events.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 10 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Create meeting, record decision; owner portfolio and manager dashboard (mockups 05–09).

## Decision gates before or during this sprint
- none beyond each story's blocking questions

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
