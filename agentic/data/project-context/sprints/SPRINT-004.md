# SPRINT-004: Leases and reporting foundation

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Leases can be drafted and activated with a rent schedule; reporting-service consumes unit/ownership events; quality gates guard contracts and permissions.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-003-S02 | [BOS-003 STORY-002](../features/BOS-003/stories/STORY-002.md) | L | BE/DB, Mobile, QA | Q-BOS-003-07, -08, -12, -13, -14 |
| BOS-003-S03 | [BOS-003 STORY-003](../features/BOS-003/stories/STORY-003.md) | L | BE/DB, Mobile, QA | Q-BOS-003-14, -15, -16, -17, -25 |
| BOS-008-S01 | [BOS-008 STORY-001](../features/BOS-008/stories/STORY-001.md) | L | BE/Integration, Mobile, QA | Q-BOS008-01 (new reporting-service), -02, -04, -06, -18 |
| BOS-009-S06 | [BOS-009 STORY-006](../features/BOS-009/stories/STORY-006.md) | L | BE, QA | Q-BOS-009-07, -14 |

## Order and Dependencies
BOS-003 S02 → S03. BOS-008 S01 needs BOS-013 S04 + BOS-009 S05. BOS-009 S06 independent.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 12 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Mobile: lease list, create lease, lease detail (mockups 20–22).

## Decision gates before or during this sprint
- none beyond each story's blocking questions

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
