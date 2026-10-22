# SPRINT-013: Exports, projection resilience, CI/CD

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Core financial reports export (§146.22); projections rebuild safely; CI/CD and environments exist.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-008-S08 | [BOS-008 STORY-008](../features/BOS-008/stories/STORY-008.md) | L | BE, Mobile, QA | Q-BOS008-03, -07, -11, -16 |
| BOS-008-S09 | [BOS-008 STORY-009](../features/BOS-008/stories/STORY-009.md) | M | BE, QA | Q-BOS008-17 |
| BOS-009-S07 | [BOS-009 STORY-007](../features/BOS-009/stories/STORY-007.md) | XL | DevOps, QA | Q-BOS-009-08 (production topology), -09, -10, -14 |

## Order and Dependencies
BOS-008 S08 after S07; S09 after S02–S07. BOS-009 S07 may split into 007a CI (pull forward if possible) and 007b CD/topology.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 10 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
PDF/Excel export, owner statement.

## Decision gates before or during this sprint
- Production topology (BOS-009 Q-08) before this sprint

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
