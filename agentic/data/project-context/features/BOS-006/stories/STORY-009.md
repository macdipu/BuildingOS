# STORY-009: Resident issue reporting (conditional)

Status: PROPOSED (planning draft 2026-10-22; not approved). **Blocked by Q-BOS-006-11**: BRD §138
grants OWNER/TENANT no `work.*` permission; build only if the operator approves resident work
requests. Size: M.

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Owners/tenants report an issue for their unit; staff triage it into the normal work-order flow.

## Scope (if approved, Q-BOS-006-11 option A)
- Use case ReportIssue: creates a work order in DRAFT with `reported_by` and unit reference
  (§8.5 "Work requests").
- Flutter Report Issue screen (mobile/34): unit picker (own units/lease only), category, priority,
  description, attachments.
- "My reports" list with status for the reporter.

## Acceptance Criteria
1. Fields: unit, category (Q-BOS-006-05), priority `LOW/MEDIUM/HIGH/CRITICAL` (§27), description,
   attachments (§27, gap 34); reporters cannot set contractor, estimated cost or planned start
   (gap 34 BRD-only note).
2. A reporter can choose only units they own (building-service ownership) or lease (BOS-003), in the
   active building (§5, §138 "owned units"/"self").
3. Reporters see only their own reports and their status; never other residents' work or costs.
4. Staff see reported items in the Work Order List (STORY-002) as DRAFT and continue with STORY-003.
5. `work.created` emitted with a reporter flag; audited.
6. UI-only items not built: room-level location, hashtag tags, SLA-hour framing, "Pre-verified unit
   ledger" badge (gap 34).

## Tasks
- BE — reporter permission, ReportIssue + MyReports use cases, unit-ownership check (M)
- Mobile — Report Issue screen + My reports list (M)
- QA — reporter isolation and unit-ownership tests (S)

## Dependencies
STORY-002, STORY-004 (attachments). Q-BOS-006-11, -05. Lease-based reporting needs BOS-003.

## Out of Scope
Resident rating/sign-off (gap 38), notifications to reporter (BOS-007).

## References
BRD §5, §8.5, §27, §73, §138; ui/stitch/gaps/mobile-ops-community.md (34).
