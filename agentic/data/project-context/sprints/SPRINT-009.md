# SPRINT-009: Work orders and notices

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Building work can be created and tracked; notices can be published and read.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-006-S01 | [BOS-006 STORY-001](../features/BOS-006/stories/STORY-001.md) | L | BE/DB, Mobile, QA | Q-BOS-006-01 (new work-service), -02, -03, -16, -18 |
| BOS-006-S02 | [BOS-006 STORY-002](../features/BOS-006/stories/STORY-002.md) | L | BE, Mobile, QA | Q-BOS-006-04, -05, -16, -18 |
| BOS-006-S03 | [BOS-006 STORY-003](../features/BOS-006/stories/STORY-003.md) | L | BE, Mobile, QA | Q-BOS-006-04, -10, -19 |
| BOS-007-S01 | [BOS-007 STORY-001](../features/BOS-007/stories/STORY-001.md) | L | BE/DB, Mobile, QA | Q-BOS-007-01 (new communication-service), -03, -04, -05, -07 |

## Order and Dependencies
BOS-006 S01 → S02 → S03; BOS-007 S01 parallel.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 12 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Work order create/assign/complete (mockups 35–38); notice board (mockups 39–40).

## Decision gates before or during this sprint
- BOS-006 Q-01/-02/-04 and BOS-007 Q-01/-03..-07 before this sprint

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
