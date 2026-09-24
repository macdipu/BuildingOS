# BOS-010 F2 — Building application & lifecycle

Status: REQUIREMENTS_READY (revision 1, 2026-09-24).
Run: `RUN-5AC974860CB74DA790FDBA00532A15A2` (new_feature, FEATURE, NO_REPLAN).
Source: BRD §149.3–149.7, §110.2, §149.22; parent [REQUIREMENTS.md](../REQUIREMENTS.md) BA-01..BA-05
as amended by [DECISIONS.md](../DECISIONS.md) D-07, D-12..D-14, D-24, D-26, D-28..D-32.
Depends on F5a (creation-fee status API, [TECH-SPEC](../subscription-plans/TECH-SPEC.md) §5).

## Scope

| ID | Requirement and acceptance | Source |
|---|---|---|
| AP-01 | `BuildingApplication` owned by building-service: `application_number` (unique, human-readable), `applicant_user_id` (token `sub`), `building_name`, `building_type` (`RESIDENTIAL`, `COMMERCIAL`, `MIXED`), `address`, `area`, `district`, optional `postal_code`, optional `total_floors` (>0), `estimated_units` (>0), `applicant_relationship` (D-31; `OTHER` requires a note), `contact_name`, `contact_phone` (canonical `01XXXXXXXXX`, TASK-007), optional `contact_email`, optional `management_type` (D-32), optional `latitude`/`longitude` (both or neither), `source` (`SELF_SERVICE` only this slice), `status`, submit/review timestamps and reviewer, `rejection_reason`. | §149.3, D-31, D-32 |
| AP-02 | Customer (any authenticated user) creates a draft, edits it while `DRAFT` or `MORE_INFORMATION_REQUIRED`, submits it, lists their own applications and reads one. Submit validates all required fields. A user sees only their own applications. | §149.3–149.4, D-14 |
| AP-03 | State machine `DRAFT → SUBMITTED → UNDER_REVIEW → {MORE_INFORMATION_REQUIRED → SUBMITTED, REJECTED, APPROVED}`. Illegal transitions → 409 with explicit code. Request-information needs a message; reject needs a reason. A submitted application is never a building. | §149.4, BA-02 |
| AP-04 | Every state change (application and building) writes an audited transition row: from, to, actor, reason/message, time. History readable by the applicant (without internal notes) and by platform admins. Kafka lifecycle events are out of scope (D-13). | §149.4, D-13 |
| AP-05 | Verification documents: applicant uploads/removes files while `DRAFT`/`MORE_INFORMATION_REQUIRED`; applicant and platform admins download. Stored in S3-compatible storage behind a port (MinIO local, S3 prod). Size and content-type limits configurable. | §149.3, D-14, D-28 |
| AP-06 | Back-office review API (UI is a later slice): list by status, detail, history, internal notes (add/list; never shown to applicant), actions Start Review / Request More Information / Reject / Approve. `SUPER_ADMIN` and `PLATFORM_ADMIN` identically; others 403. Sensitive actions (approve, reject, suspend, reactivate) require an audit reason. | §149.5, BA-04, BA-05, D-07 |
| AP-07 | Duplicate-building signals for an application under review: other non-rejected applications and existing buildings matching normalized name, normalized address+area+district, contact phone, or coordinates within a configurable radius. Returned as review signals with the matched fields; never merged or blocking. | §149.6, D-14 |
| AP-08 | Approve (from `UNDER_REVIEW`): requires creation fee status `SETTLED` or `NOT_REQUIRED` from subscription-service (`UNPAID` → 409 `CREATION_FEE_UNPAID`, `FEE_NOT_CONFIGURED` passed through, service down → 503, no state change). Request carries the initial admin phone (review UI pre-fills applicant contact phone, D-12). Auth-service finds-or-creates that user (D-29). Then in one local transaction: application `APPROVED`, canonical `Building` created in `ONBOARDING` from the application data, `BUILDING_ADMIN` membership for that user, transitions audited. No subscription attached (D-26). | §149.7, D-12, D-26, D-29 |
| AP-09 | Building lifecycle actions (platform admins): `Activate` `ONBOARDING → ACTIVE` when an active `BUILDING_ADMIN` exists (interim, D-30); `Suspend` `ACTIVE → SUSPENDED` and `Reactivate` `SUSPENDED → ACTIVE`, each with reason. Suspension deletes nothing. `ARCHIVED` is out of scope. | §149.4, D-14, D-30 |
| AP-10 | Flutter customer flow in `user_app`: My Building Applications list with status; create/edit draft form (fields of AP-01, client validation mirroring server); attach/remove documents (camera/gallery/file); submit; show reviewer message when more information is required and allow resubmit. English and Bangla strings. | D-14, BL-01 (applications part) |

## Out of scope

Back-office UI; `Assign Onboarding Agent` and assisted onboarding (D-10, G3); sources other than
`SELF_SERVICE`; `Open Applicant Profile`; archive; Kafka outbox (D-13); onboarding steps and the
≥1-unit activation prerequisite (D-30); membership invitations; directing a duplicate applicant to
request membership in an existing building.
