# SPRINT-005: Lease lifecycle and rent invoices

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Leases end correctly (notice, termination, move-out, renewal) and monthly rent invoices are generated idempotently and visible.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-003-S04 | [BOS-003 STORY-004](../features/BOS-003/stories/STORY-004.md) | L | BE, Mobile, QA | Q-BOS-003-18, -19, -20, -21 |
| BOS-003-S05 | [BOS-003 STORY-005](../features/BOS-003/stories/STORY-005.md) | L | BE/DB, Mobile, QA | Q-BOS-003-02 (invoice ownership, hard), -20, -22, -23, -24 |
| BOS-003-S06 | [BOS-003 STORY-006](../features/BOS-003/stories/STORY-006.md) | M | Mobile, BE, QA | Q-BOS-003-02, -08, -28 |
| BOS-009-S03 | [BOS-009 STORY-003](../features/BOS-009/stories/STORY-003.md) | M | BE, Security, QA | Q-BOS-009-04, -02 |

## Order and Dependencies
S04 and S05 in parallel after S03; S06 after S05. Gateway hardening independent.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 10 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Rent overview and invoice detail (mockups 23–24); generate invoices twice → no duplicates.

## Decision gates before or during this sprint
- BOS-004 Q-01..-09, -25, -30, -31 before SPRINT-006

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
