# SPRINT-008: Reversal, advance, rent collection

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Finance core complete: reversal, advance consumption, maintenance dashboard and rent collection; refresh tokens.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-004-S04 | [BOS-004 STORY-004](../features/BOS-004/stories/STORY-004.md) | M | BE, Mobile, QA | Q-BOS-004-03, -13, -14, -15 |
| BOS-004-S05 | [BOS-004 STORY-005](../features/BOS-004/stories/STORY-005.md) | M | BE, Mobile, QA | Q-BOS-004-11, -12 |
| BOS-004-S06 | [BOS-004 STORY-006](../features/BOS-004/stories/STORY-006.md) | M | BE, Mobile, QA | Q-BOS-004-23, -24, -29 |
| BOS-004-S08 | [BOS-004 STORY-008](../features/BOS-004/stories/STORY-008.md) | L | BE, Mobile, QA | Q-BOS-004-03, -11, -21, -22 |
| BOS-009-S02 | [BOS-009 STORY-002](../features/BOS-009/stories/STORY-002.md) | L | BE, Mobile, Web, QA | Q-BOS-009-03 |

## Order and Dependencies
S04, S05, S06 after S02; S08 after S02/S03 and BOS-003 S05.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 12 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Collect rent full/partial/advance, reverse payment (mockups 25, 27, 28). **BOS-004 epic UAT; BRD §143B/C scenarios.**

## Decision gates before or during this sprint
- none beyond each story's blocking questions

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
