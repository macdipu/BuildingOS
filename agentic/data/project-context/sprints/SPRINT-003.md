# SPRINT-003: Role UI, consumer framework, rental foundation

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Role administration is usable from the app and console; the platform has a retry/DLQ consumer framework; rental-service and the tenant registry ship.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-013-S05 | [BOS-013 STORY-005](../features/BOS-013/stories/STORY-005.md) | M | Mobile, QA | Q-BOS-013-13, -03 |
| BOS-013-S06 | [BOS-013 STORY-006](../features/BOS-013/stories/STORY-006.md) | M | BE, Web, QA | Q-BOS-013-03, -06 |
| BOS-009-S05 | [BOS-009 STORY-005](../features/BOS-009/stories/STORY-005.md) | L | BE/Integration, Web, QA | Q-BOS-009-06 |
| BOS-003-S01 | [BOS-003 STORY-001](../features/BOS-003/stories/STORY-001.md) | XL | BE/DB, Mobile, QA | Q-BOS-003-01 (new rental-service), -04, -05, -06, -08, -09, -10, -11 |

## Order and Dependencies
BOS-009 S05 before any real consumer (BOS-008 S01). BOS-003 S01 needs BOS-013 S01/S03.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 12 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Mobile: members & roles screen, per-role nav; tenant list/add/detail (mockups 17–19).

## Decision gates before or during this sprint
- BOS-003 Q-02 (rent invoice ownership) before SPRINT-005

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
