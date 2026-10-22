# SPRINT-001: F6 close-out and decision gate

Status: COMMITTED 2026-10-22 (dates not set).

## Dates
not set

## Goal
Release the back-office console (BOS-010 F6) for local/test and take the decisions that unblock MVP planning: the new-service approvals and the BOS-013/BOS-003 blocking questions.

## Committed Work Items
Proposed, not committed. Hierarchy: EPIC_STORY_TASK for each BOS epic; F6 items execute the existing BOS-010 task plan (NO_REPLAN).

| Item | Story | Size | Lanes | Blocking questions / gates |
|---|---|---|---|---|
| F6-R4 | BOS-010 F6-T10 R-4 audit paging composite cursor (contract change) | M | BE, Web, QA | operator technical approval of R-4 |
| F6-QA | BOS-010 F6-T10 live-stack QA: Playwright + role walkthrough incl. SUPPORT_AGENT session start | M | QA, Web | infra/local/.env BACKOFFICE_SERVICE_CLIENT_SECRET |
| F6-REL | BOS-010 F6 release readiness (local/test) + operator release approval | S | QA | operator release approval |
| HK | Harness housekeeping: register mvn/npm/flutter in allowed-commands.json, python3.11 for CLI (kit change) | S | DevOps | BOS-009 Q-15 (kit change needs operator approval) |
| BOS-009-S04 | [BOS-009 STORY-004](../features/BOS-009/stories/STORY-004.md) | L | BE, DevOps, QA | Q-BOS-009-05 |

## Order and Dependencies
F6-R4 → F6-QA → F6-REL (R-4 can be deferred: then QA runs on the current audit paging). BOS-009 STORY-004 (observability) and HK are independent lanes.

## Lanes / Owners
Agent personas per lane (agentic/kit/agents/): BE/DB → backend, Mobile → Flutter, Web → back-office web, QA → automated-qa-agent, DevOps/Security as named. Human owners: not assigned.

## Capacity and Uncertainty
Relative load 9 units (S=1, M=2, L=3, XL=5). Capacity is unknown: no velocity has been measured. SPRINT-001 is the calibration sprint; resize later sprints from its actual throughput. Each story re-enters the harness at its own run (intake → requirements → technical approval → tasks) before code, so open questions can move work between sprints.

## UAT Target
Back-office console on the local stack: SUPER_ADMIN, PLATFORM_ADMIN, SUPPORT_AGENT, ONBOARDING_AGENT, SUBSCRIPTION_ADMIN walkthroughs.

## Decision gates before or during this sprint
- Approve or defer R-4
- Approve new services: rental, finance, work, communication, notification, reporting (each epic's Q-01)
- Answer BOS-013 Q-01, -03, -04, -07, -09, -10 and BOS-003 Q-01, -04, -06, -08, -09, -11

## Commitment
Committed by macdipu (operator) 2026-10-22 via AskUserQuestion in the Claude Code session: "Commit SPRINT-001 + start (Recommended)". Same session approved R-4, the six new services and all BOS-013 defaults.

## Changes
(none)
