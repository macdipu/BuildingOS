# SPRINT-007: Payments and expenses

Status: PROPOSED (planning draft 2026-10-22). Not committed: commitment is an operator decision.

## Dates
not set

## Goal
Maintenance payments with allocation, advance credit, server receipts and PDF; expenses with the BRD status enum.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| BOS-004-S02 | [BOS-004 STORY-002](../features/BOS-004/stories/STORY-002.md) | L | BE/DB, Mobile, QA | Q-BOS-004-03, -04, -05, -10, -11, -25, -18 |
| BOS-004-S03 | [BOS-004 STORY-003](../features/BOS-004/stories/STORY-003.md) | L | BE, Mobile, QA | Q-BOS-004-28, -29 |
| BOS-004-S07 | [BOS-004 STORY-007](../features/BOS-004/stories/STORY-007.md) | L | BE/DB, Mobile, QA | Q-BOS-004-03, -16..-21, -23 |
| BOS-009-S01 | [BOS-009 STORY-001](../features/BOS-009/stories/STORY-001.md) | M | BE, Security, QA | Q-BOS-009-01 (SMS vendor), -02 |

## Order and Dependencies
S02 → S03; S07 parallel after S01. SMS adapter independent (needs the vendor decision).

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 11 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Record maintenance payment, receipt PDF, expense add/approve/pay (mockups 26, 30–33).

## Decision gates before or during this sprint
- none beyond each story's blocking questions

## Commitment
Not committed. Record who committed, when, and the chat quote here.

## Changes
(none)
