# SPRINT-012: Remaining dashboards and reports

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Tenant, committee and main dashboards, the reports screen, and notice attachments.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-008-S04 | [BOS-008 STORY-004](../features/BOS-008/stories/STORY-004.md) | M | BE, Mobile, QA | Q-BOS008-03, -10, -11, -12 |
| BOS-008-S05 | [BOS-008 STORY-005](../features/BOS-008/stories/STORY-005.md) | M | BE, Mobile, QA | Q-BOS008-03, -13, -14, -15 |
| BOS-008-S06 | [BOS-008 STORY-006](../features/BOS-008/stories/STORY-006.md) | M | BE, Mobile, QA | Q-BOS008-03, -06, -12 |
| BOS-008-S07 | [BOS-008 STORY-007](../features/BOS-008/stories/STORY-007.md) | L | BE, Mobile, QA | Q-BOS008-03, -05, -07 |
| BOS-007-S03 | [BOS-007 STORY-003](../features/BOS-007/stories/STORY-003.md) | M | BE, Mobile, QA | Q-BOS-007-11 |

## Order and Dependencies
S04–S06 in parallel after S01; S07 after S02/S05.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 11 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
All four role dashboards (§146.21); §116 reports.

## Decision gates before or during this sprint
- none beyond each story's blocking questions

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
