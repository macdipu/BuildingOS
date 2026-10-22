# STORY-009: Load testing and MVP release evidence

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Produce the evidence that BuildingOS meets §134 performance targets and §142 NFRs on staging,
and a §146 release evidence matrix mapping each MVP success criterion to proof. BACKLOG:
§146 #24 "verified in BOS-009".

## Scope
- Load test suite (tool per Q-BOS-009-13) for delivered flows: OTP login, building selection,
  unit/ownership reads and writes, back-office lists; profile derived from operator-supplied
  expected volume, never invented.
- Measure against §134: typical API P95 < 500 ms (excluding file/report generation);
  dashboard P95 < 1 s once BOS-008 read models exist; mobile list first cached render.
- Failure-recovery drills (§142 Reliability/Availability): kill a service, kill Kafka broker,
  DB failover (as topology allows) - verify graceful degradation of non-financial reads and no
  lost outbox events.
- §146 evidence matrix in this feature folder: criteria 1-24 -> test/run id/record; criteria
  owned by unbuilt epics (11-22 mostly BOS-003/004/006/007/008) marked NOT YET EVIDENCED.
  Criterion 7 is evidenced against the per-user model (D-22..D-27), which supersedes the BRD
  building-subscription wording; that deviation is stated, not hidden.
- Localization check (§142 "Bangla/English-ready"): string externalization audit of user_app
  and back-office web (report only).

## Acceptance Criteria
1. Load test report on staging shows P95 per endpoint group vs §134 target; any miss is
   recorded as a defect, not waived (§134).
2. "No duplicate financial transaction after retry" (§134, §142) is listed as pending until
   BOS-004; once finance exists the test is part of the suite.
3. Service-kill drill: other services' non-financial reads keep responding or degrade with a
   normalized error (§142 Availability); outbox events created during a Kafka outage are
   published after recovery (§15).
4. Evidence matrix covers all 24 §146 criteria with status EVIDENCED / NOT YET EVIDENCED /
   DEVIATION (D-22..D-27) and a link per evidenced item (§146).
5. §146 #24 audit trace: references STORY-006 audit completeness results for all implemented
   critical actions (§114, §149.21).
6. No production-readiness claim is made while any criterion in the platform-owned set
   (1-10, 23-24) is NOT YET EVIDENCED (ARCHITECTURE.md "No general production-readiness claim").

## Tasks
Proposed task lanes only:
- QA: load test scripts + staging run + report - M
- QA/Infra: failure-recovery drills - M
- QA: §146 evidence matrix + localization audit report - S

Size: L (uncertainty: high; volume targets unknown, full §146 depends on other epics).

## Dependencies
- Hard: STORY-007 (staging), STORY-004 (metrics), STORY-006 (audit/permission results),
  Q-BOS-009-13. Full closure: BOS-003, BOS-004, BOS-006, BOS-007, BOS-008.

## Out of Scope
- Chat/push NFRs (§150.12, BOS-007). Capacity planning for scale beyond the stated volume.

## References
BRD §15, §114, §134, §142, §146, §149.21, §150.12; features/BOS-001/BACKLOG.md
(§146 coverage); BOS-010 DECISIONS D-22..D-27; agentic/data/project-context/ARCHITECTURE.md.
