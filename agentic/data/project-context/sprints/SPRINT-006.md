# SPRINT-006: Rental completion and finance foundation

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Rental core is complete (adjustments, owner and tenant access); finance-service ships with maintenance fee generation.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-003-S07 | [BOS-003 STORY-007](../features/BOS-003/stories/STORY-007.md) | M | BE, Mobile, QA | Q-BOS-003-25, -26, -27 |
| BOS-003-S08 | [BOS-003 STORY-008](../features/BOS-003/stories/STORY-008.md) | M | BE, Mobile, QA | Q-BOS-003-08, -09, -18 |
| BOS-003-S09 | [BOS-003 STORY-009](../features/BOS-003/stories/STORY-009.md) | L | BE, Mobile, QA | Q-BOS-003-05, -06, -09, -22, -29 |
| BOS-004-S01 | [BOS-004 STORY-001](../features/BOS-004/stories/STORY-001.md) | XL | BE/DB, Mobile, QA | Q-BOS-004-01 (new finance-service), -02, -03, -06, -07, -08, -09, -25, -30, -31 |

## Order and Dependencies
BOS-003 S07–S09 after S05/S06; BOS-004 S01 is a separate lane.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 12 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Tenant invitation → my lease, my invoices; owner unit rental tabs; maintenance fee preview/generate (mockup 29). **BOS-003 epic UAT.**

## Decision gates before or during this sprint
- none beyond each story's blocking questions

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
