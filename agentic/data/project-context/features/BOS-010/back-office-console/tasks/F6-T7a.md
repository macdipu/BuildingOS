# F6-T7a — building-service list endpoint

## Category
BE

## Objective
Platform building list by lifecycle status for back-office Buildings sections (D-34).

## Scope
`GET /api/v1/platform/buildings?status=&q=&page=&size=` (status ∈ ONBOARDING/ACTIVE/SUSPENDED, q = name/holding contains); use case `listplatformbuildings`; repository query; OpenAPI entry; SUPER_ADMIN/PLATFORM_ADMIN only.

## Dependencies
Technical re-approval (changes approved TECH-SPEC); F6-T1 conventions.

## Implementation Requirements
One use case per action; repository pattern; Clean Architecture per docs/ARCHITECTURE.md.

## Acceptance Criteria
- Returns paged buildings filtered by status and q; default sort name asc.
- Other callers 403; invalid status 400.
- ArchUnit + existing tests green.

## Test Requirements
Use-case unit tests; Testcontainers integration test; `mvn -pl building-service verify`.

## UI Reference
Serves `ui/stitch/back-office/02-building-onboarding-pipeline` building lists; BRD §149.2, §149.4, §118.

## References
DECISIONS.md D-34/D-35; DELIVERY-PLAN.md.

## Out of Scope
Frontend (F6-T7).
