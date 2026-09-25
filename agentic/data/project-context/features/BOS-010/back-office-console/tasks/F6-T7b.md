# F6-T7b — subscription-service list endpoint

## Category
BE

## Objective
Platform-wide list of user subscriptions for back-office Subscriptions sections (D-35, per-user model D-22).

## Scope
`GET /api/v1/platform/subscriptions?status=&plan=&page=&size=`; use case `listplatformsubscriptions`; repository query; OpenAPI entry; SUPER_ADMIN/PLATFORM_ADMIN/SUBSCRIPTION_ADMIN.

## Dependencies
Technical re-approval (changes approved TECH-SPEC); F6-T1 conventions.

## Implementation Requirements
One use case per action; repository pattern; Clean Architecture per docs/ARCHITECTURE.md.

## Acceptance Criteria
- Paged list with user id, plan, status, dates; filters by status and plan.
- Other callers 403.
- Existing tests green.

## Test Requirements
Use-case unit tests; Testcontainers integration test; `mvn -pl subscription-service verify`.

## UI Reference
Serves `ui/stitch/back-office/03-society-subscriptions-billing` table layout; BRD §149.2, §149.15; D-22..D-27.

## References
DECISIONS.md D-34/D-35; DELIVERY-PLAN.md.

## Out of Scope
Frontend (F6-T7).
