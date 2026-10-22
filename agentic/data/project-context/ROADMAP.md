# BuildingOS delivery roadmap

Status: PROPOSED (planning draft 2026-10-22). Nothing here is approved or committed; dates and capacity are not set.
Source: BRD §123 (sequence), §124 (MVP), §146 (MVP success criteria), [epic backlog](features/BOS-001/BACKLOG.md), UI conflict decisions C-1..C-13 (operator 2026-09-25).

## Delivered

BOS-001 foundation · BOS-002/BOS-010 identity, OTP, platform roles · F5a subscriptions (per-user, D-22) · F2 building application & lifecycle · F4 units & ownership · BOS-011/012 Stitch UI · F6 back-office console (in QA, SPRINT-001).

## Epics

| Epic | Outcome | Stories | Relative size | Open questions | New service (needs technical approval) |
|---|---|---|---|---|---|
| [BOS-013](features/BOS-013/EPIC.md) | Building roles, role assignment, membership lookup, building-service events | 6 | 14 | 15 + consolidated | none (extends building-service; D-38 amendment) |
| [BOS-003](features/BOS-003/EPIC.md) | Tenants, leases, rent schedule, rent invoices | 9 | 26 | 29 | rental-service |
| [BOS-004](features/BOS-004/EPIC.md) | Payments, allocation, advance, receipts, reversal, maintenance fees, expenses | 8 | 23 | 31 | finance-service |
| [BOS-006](features/BOS-006/EPIC.md) | Work orders, contractors, assets, expense linkage | 9 | 21 | 20 | work-service |
| [BOS-007](features/BOS-007/EPIC.md) | Notices, meetings, notifications/push (chat deferred) | 9 | 26 | 23 | communication-service, notification-service |
| [BOS-008](features/BOS-008/EPIC.md) | Projections, role dashboards, reports, PDF/Excel | 9 | 22 | 18 | reporting-service |
| [BOS-009](features/BOS-009/EPIC.md) | Security, observability, DLQ, quality gates, CI/CD, backup, load, release evidence | 9 | 27 | 15 | possibly audit-service (Q-07) |

Each epic folder holds `EPIC.md`, `stories/STORY-*.md` (acceptance criteria cite the BRD; proposed task lanes) and `OPEN-QUESTIONS.md` (options + recommended default; not decisions).

## Sprint plan

| Sprint | Name | Epics | Load |
|---|---|---|---|
| [SPRINT-001](sprints/SPRINT-001.md) | F6 close-out and decision gate | BOS-009, BOS-010 | 9 |
| [SPRINT-002](sprints/SPRINT-002.md) | Building roles and platform events | BOS-013 | 10 |
| [SPRINT-003](sprints/SPRINT-003.md) | Role UI, consumer framework, rental foundation | BOS-003, BOS-009, BOS-013 | 12 |
| [SPRINT-004](sprints/SPRINT-004.md) | Leases and reporting foundation | BOS-003, BOS-008, BOS-009 | 12 |
| [SPRINT-005](sprints/SPRINT-005.md) | Lease lifecycle and rent invoices | BOS-003, BOS-009 | 10 |
| [SPRINT-006](sprints/SPRINT-006.md) | Rental completion and finance foundation | BOS-003, BOS-004 | 12 |
| [SPRINT-007](sprints/SPRINT-007.md) | Payments and expenses | BOS-004, BOS-009 | 11 |
| [SPRINT-008](sprints/SPRINT-008.md) | Reversal, advance, rent collection | BOS-004, BOS-009 | 12 |
| [SPRINT-009](sprints/SPRINT-009.md) | Work orders and notices | BOS-006, BOS-007 | 12 |
| [SPRINT-010](sprints/SPRINT-010.md) | Notification delivery and work-cost linkage | BOS-006, BOS-007 | 10 |
| [SPRINT-011](sprints/SPRINT-011.md) | Meetings and first dashboards | BOS-007, BOS-008 | 10 |
| [SPRINT-012](sprints/SPRINT-012.md) | Remaining dashboards and reports | BOS-007, BOS-008 | 11 |
| [SPRINT-013](sprints/SPRINT-013.md) | Exports, projection resilience, CI/CD | BOS-008, BOS-009 | 10 |
| [SPRINT-014](sprints/SPRINT-014.md) | MVP release readiness | BOS-009 | 6 |

Load is relative units (S=1, M=2, L=3, XL=5). No velocity exists yet; SPRINT-001 calibrates it.

## MVP coverage (BRD §146)

| §146 | Criterion | Delivered by |
|---|---|---|
| 1–6 | Identity, application, review, initial admin, onboarding, scoped support | Delivered (BOS-010); F6 console SPRINT-001 |
| 7 | Subscription plan/status/entitlements | Delivered per-user (D-22 supersedes building-level) |
| 8–10 | Units, ownership, My Properties | Delivered (F4); role-aware access BOS-013 |
| 11–13 | Tenants, leases, monthly rent | BOS-003 (SPRINT-003..006) |
| 14–18 | Collect rent, receipts, maintenance fees, expenses, outstanding | BOS-004 (SPRINT-006..008) |
| 19 | Create and track building work | BOS-006 STORY-001..003 (SPRINT-009) |
| 20 | Announcements and meeting decisions | BOS-007 STORY-001, -002, -006, -007 (SPRINT-009..011) |
| 21 | Role-specific dashboards | BOS-008 STORY-002..006 (SPRINT-011..012) |
| 22 | Export core financial reports | BOS-008 STORY-007..008 (SPRINT-012..013) |
| 23–24 | History and audit trace | Every epic's audit criteria; verified by BOS-009 STORY-006, -009 |

## Post-MVP backlog

| Item | Story | Size | Blocking |
|---|---|---|---|
| BOS-006-S04 | [BOS-006 STORY-004](../features/BOS-006/stories/STORY-004.md) | M | Q-BOS-006-15 |
| BOS-006-S05 | [BOS-006 STORY-005](../features/BOS-006/stories/STORY-005.md) | M | Q-BOS-006-20 |
| BOS-006-S06 | [BOS-006 STORY-006](../features/BOS-006/stories/STORY-006.md) | M | Q-BOS-006-06, -07, -18 |
| BOS-006-S07 | [BOS-006 STORY-007](../features/BOS-006/stories/STORY-007.md) | M | Q-BOS-006-08, -09 |
| BOS-006-S09 | [BOS-006 STORY-009](../features/BOS-006/stories/STORY-009.md) | M | Q-BOS-006-11 (hard: no work.* permission for owners/tenants) |
| BOS-007-S08 | [BOS-007 STORY-008](../features/BOS-007/stories/STORY-008.md) | XL | Q-BOS-007-22 (chat MVP inclusion; default deferred), -03, -05, -21 |
| BOS-007-S09 | [BOS-007 STORY-009](../features/BOS-007/stories/STORY-009.md) | L | Q-BOS-007-22 |
| F6-T4b | BOS-010 F6-T4b support action relay to owning services | L | plan + technical approval; needs target endpoints from BOS-003/004 |

Future scope with no work items: polls/AGM voting, visitor gate pass (C-13, BRD §3.2); §125 exclusions (automated billing, payment gateway, OCR, double-entry ledger, AI assistant).

## Decision gates (operator)

1. **Now (SPRINT-001):** R-4; new-service approvals; BOS-013 Q-01/-03/-04/-07/-09/-10; BOS-003 Q-01/-04/-06/-08/-09/-11.
2. **Before SPRINT-005:** BOS-003 Q-02 (who owns rent invoices: rental vs finance), Q-22, Q-23.
3. **Before SPRINT-006:** BOS-004 Q-01..-09, -25, -30, -31 (receipt format, maintenance rules, finance roles).
4. **Before SPRINT-009:** BOS-006 Q-01/-02/-04; BOS-007 Q-01, -03..-07.
5. **Before SPRINT-013:** production topology and hosting (BOS-009 Q-08), SMS vendor (Q-01, earlier if possible).
6. **Before MVP release:** RPO/RTO, retention, load volumes (BOS-009 Q-11..-13); chat inclusion (BOS-007 Q-22, default deferred).

## Decision log

- 2026-10-22 R-4 approved (BOS-010 D-39) — "Approve R-4 (Recommended)".
- 2026-10-22 New deployables approved: rental, finance, work, communication, notification, reporting — "Approve all six (Recommended)"; TECH-SPEC review per service still required.
- 2026-10-22 BOS-013 open questions: all recommended defaults accepted (BOS-013 D-01, D-02) — "Accept all defaults (Recommended)".
- 2026-10-22 SPRINT-001 committed — "Commit SPRINT-001 + start (Recommended)".

## Working rules

- A sprint is proposed until the operator commits it in its `## Commitment` section.
- Each story runs through the harness (intake → requirements → technical approval → tasks → implementation → review → QA → release); planning files here are inputs, not approvals.
- Open questions are answered in the epic's `OPEN-QUESTIONS.md` with the operator's quote; a story starts only when its blocking questions are answered.
- Audit, authorization, tenant isolation, outbox and idempotency ship with each story, not later in BOS-009.
