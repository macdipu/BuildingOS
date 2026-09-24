# BOS-010 F2 — Code review and automated QA

Run `RUN-5AC974860CB74DA790FDBA00532A15A2`. 2026-09-24. Revision: HEAD `8e66061` + uncommitted F2 working tree.
Verdicts: review **READY**, automated QA **READY** (agent verdicts, not human approval). Manual device QA and
the release decision belong to the operator.

## Code review

Two independent read-only reviews (backend; Flutter), each finding checked against the code.

| Check | Result |
|---|---|
| Layering | building-service features `buildingapplication`, `document`, `building`, `duplicate`, `note`, `shared`: pure domain, one use case per action, repositories/ports behind interfaces, adapters in infrastructure. ArchUnit rules unchanged and passing. Flutter feature `building_application`: pure-Dart domain, no cross-feature imports (layer-rules test passes). |
| Authorization | `ApplicationAccess` on every application use case; another user's application is 404, never 403. Review, notes, duplicates and building lifecycle require `SUPER_ADMIN`/`PLATFORM_ADMIN`. auth-service provisioning requires the same roles on the relayed token. |
| Consistency | Every transition under `SELECT … FOR UPDATE`; approve does remote checks first and all writes in one transaction; `building.application_id UNIQUE`. Concurrency tests: start-review and approve → exactly one winner. |
| Uploads | Type by magic bytes (PDF/JPEG/PNG), size and count limits under the application lock; object key never derived from the file name; downloads `attachment` + `nosniff`. |
| Fail closed | Unpaid fee → `CREATION_FEE_UNPAID`; fee not configured → `FEE_NOT_CONFIGURED`; dependency down → 503 with no state change. |
| Shared changes | Gateway `building-api` route; `ApiClient` keeps the error body on 4xx (auth callers read only `message`, unaffected); OpenAPI `ApiError.code` now a documented string. |

Findings:
1. Backend review: no defects found.
2. Flutter, fixed: `ApplicationDetailController` cast `Get.arguments as String`; a missing/invalid argument now
   shows "Application not found" instead of throwing in `onInit`.
3. Flutter, no change needed (4 findings): non-null casts in `ApplicationDocumentModel.fromJson` — a malformed
   body raises `TypeError`, which the repository's `_guard` maps to a `ParsingFailure` (localized "Something went
   wrong"); no crash path.
4. Deviations from TECH-SPEC text are recorded in its §12 (package name, one migration per task, drafts hidden
   from the review queue, smoke script instead of `verify-platform.sh` services, customers cannot download).
5. Accepted risks: no virus scanning of uploads (required before production); token relay ties outbound calls
   to the approver's token lifetime; duplicate matching is heuristic (signals only); Bangla strings not yet
   reviewed by a native speaker; activation checks only the building admin until the units slice (D-30).
6. Pre-existing, not fixed: unknown gateway paths return 500 instead of 404 (platform-web catch-all).
7. Process: the implementation task ran as one ~5 h task and exceeded `max_task_seconds` (900); recorded as a
   failed task; operator raised the budget to 3600 s and the evidence was resubmitted. Future slices: one task
   per implementation step.

## Automated QA

| Requirement | Evidence | Result |
|---|---|---|
| AP-01, AP-02 | `BuildingApplicationTest` (7), `BuildingApplicationApiIntegrationTest` draft/edit/submit, invalid fields, isolation | Pass |
| AP-03 | state machine unit tests; `rejectNeedsReasonAndIsFinal`, `concurrentStartReviewHasOneWinner` | Pass |
| AP-04 | transition rows asserted per change (application and building), history endpoint | Pass |
| AP-05 | `DocumentPolicyTest` (3), `DocumentApiIntegrationTest` (4, Postgres + MinIO) | Pass |
| AP-06 | review actions 403 for non-admins, notes admin-only and hidden from applicant, queue paging, drafts hidden | Pass |
| AP-07 | `DuplicateMatcherTest` (4), `duplicateSignalsFlagOpenApplicationsAndBuildings` | Pass |
| AP-08 | `ApprovalLifecycleIntegrationTest`: happy path, token relay, unpaid/not configured, 503 no-change, concurrent approve; `UserProvisioningIntegrationTest` (3) | Pass |
| AP-09 | activate/suspend/reactivate with reasons and transitions | Pass |
| AP-10 | Flutter: validator (3), repository over real HTTP (5), form controller (4), list screen en/bn/error (3) | Pass |
| Gateway / contracts | `GatewayRoutingTest` (31, incl. 2 MB multipart byte-identical, `/internal/**` not forwarded); `check-contracts.py`; Redocly lint valid | Pass |
| End to end | `scripts/smoke-building-application.sh` against an isolated full stack (4 services from jars, Postgres, MinIO) | SMOKE PASSED |

Final run on the reviewed tree: `mvn -B -f backend/pom.xml verify` BUILD SUCCESS — auth 76, building 54,
subscription 31, gateway 31, platform-web 5 (197, 0 failures); `flutter analyze` no issues; `flutter test` 50/0.

Not covered by automation (operator): on-device check of the Flutter flow (camera/gallery/PDF pickers, Bangla
rendering), `verify-platform.sh` and the smoke script on the operator's own `.env` and volumes.
