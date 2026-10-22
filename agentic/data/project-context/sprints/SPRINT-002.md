# SPRINT-002: Building roles and platform events

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Every BRD building role exists with an agreed permission matrix, can be assigned and revoked, and other services can look up memberships and consume building-service events.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-013-S01 | [BOS-013 STORY-001](../features/BOS-013/stories/STORY-001.md) | M | BE/DB, QA | Q-BOS-013-01, -04 |
| BOS-013-S02 | [BOS-013 STORY-002](../features/BOS-013/stories/STORY-002.md) | L | BE/DB, Mobile, QA | Q-BOS-013-03, -05, -06, -12 |
| BOS-013-S03 | [BOS-013 STORY-003](../features/BOS-013/stories/STORY-003.md) | M | BE, QA | Q-BOS-013-07 (D-38 amendment, technical approval), -08, -11 |
| BOS-013-S04 | [BOS-013 STORY-004](../features/BOS-013/stories/STORY-004.md) | L | BE/Integration, QA | Q-BOS-013-09, -10, -11 |

## Order and Dependencies
S01 first; then S02, S03 and S04 in parallel (S04's role events wait for S02).

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 10 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Building admin assigns COMMITTEE/ACCOUNTANT/VIEWER/TENANT roles via API; other roles' negative tests.

## Decision gates before or during this sprint
- Event names and topic convention (BOS-013 Q-09/Q-10) before S04

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
