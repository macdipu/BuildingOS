# BOS-001 — Platform foundation
Status: TECHNICAL_READY; implementation awaiting explicit technical approval.
Request: “now read brd and start the projecet development” (2026-09-22).
Source: ../../BuildingOS_BRD_Agentic_Development.md, all 148 sections read.
Run: RUN-8446DBFAB7FD427BAC0CA569DBE104F6.

## Objective
Start BuildingOS development in the BRD's phase order (§123) with a runnable, testable local foundation. Retain user_app as the existing Flutter client. Create backend services incrementally, preserving the mandatory service and database boundaries.

## Classification
Document-first new feature; brownfield Flutter integration, greenfield backend.
BOS-001 is a bounded platform milestone with stories/tasks (STORY_TASK).
The complete product is a multi-phase programme; later phases need their own work items.
NO_REPLAN for this bounded milestone: no team capacity, sprint dates, or existing sprint are supplied. Record dependencies without inventing a sprint commitment.

## Included in this milestone
- Verified repository context and source-to-requirement traceability.
- Local Java/Spring foundation for gateway, identity and building services.
- PostgreSQL database/user isolation, Kafka local broker and health checks.
- API envelope/error conventions, security defaults, observability and test/build workflow.
- Reuse plan and baseline checks for user_app, without presenting its generic auth as BuildingOS auth.

## Excluded from this milestone
Production deployment; real SMS/Google provider integration; financial mutations; dashboards with invented totals; all future service skeletons.

## Acceptance
1. Local services build and expose health endpoints; gateway forwards an authenticated foundation request.
2. Business routes reject missing/invalid credentials; service verification does not trust the gateway alone.
3. Each domain service has an independent database role, migrations, and tests showing cross-database access is denied.
4. API contract and runbook specify reproducible commands and error cases.
5. Flutter baseline is recorded honestly; foundation work preserves its established architecture.
6. Product roadmap traces BRD MVP items and financial invariants to later delivery slices.

## Source mapping
Scope/actors: §§1–5,124–125. Boundaries: §§6–18,91,100–107,110.
Client reuse: §§31–42,97,109. Security: §§93–96.
Foundation sequence: §123. Definition of done: §§120–122.
End-to-end MVP acceptance: §§143,146.

## Decisions requiring later product input
OTP provider/limits, role bootstrap authority, lease-activation invoice ownership conflict (§17 versus §§8.3,9,91), configurable maintenance policy, and document retention.
These block affected later features, not a foundation with protected routes and no business mutation.

## Approval record
User authorized development intake and preparation from this BRD.
No scope-bound technical approval has yet been recorded; artifacts are not approval.
