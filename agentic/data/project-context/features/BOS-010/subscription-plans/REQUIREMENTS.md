# BOS-010 F5a — Revenue foundation: plans, user subscriptions, free tier, creation fee

Status: COMPLETED 2026-09-23 ([review/QA](REVIEW.md)). Requirements revision 2 (2026-09-23 — operator changed the model to user-level subscriptions;
D-22..D-27 supersede D-15, D-19, D-21).
Run: `RUN-4ABC6B2BAC974B0E878CA39C9A27AE00` (new_feature, FEATURE, NO_REPLAN).
Source: BRD §149.15–149.16, §149.19, §149.21–149.23 as amended by
[DECISIONS.md](../DECISIONS.md) D-08, D-16..D-18, D-20, D-22..D-27.
Sequenced before the building-application slice (D-11); approval there requires the creation
fee recorded here (D-26).

## Principle (D-27)

Revenue rules change often. Anything the operator may change later is **data or config**, not
code: which features are free, plan contents, fee amounts, whether a fee is required, and which
subject a subscription belongs to. Payment collection and entitlement resolution sit behind
replaceable ports so a gateway or a building-level model can be added without redesign.

## Scope

| ID | Requirement and acceptance | Source |
|---|---|---|
| RV-01 | New `subscription-service` microservice owning `subscription_db`; gateway routes; same architecture rules, health/readiness, JWT validation as existing services. | D-18 |
| RV-02 | Feature catalog (code): the §149.16 keys — booleans `maintenance.enabled`, `rent_management.enabled`, `work_orders.enabled`, `reports.pdf_export`, `reports.excel_export`, `offline_sync.enabled`; optional non-negative integers `max_units`, `max_users`, `storage_limit_mb` (absent = unlimited); `support_tier` string. Unknown keys / wrong types → 400. | §149.16, D-16 |
| RV-03 | Free tier (data): one platform record of entitlements every user gets; initial value `maintenance.enabled = true` only. Admin can view/edit it; edits audited and apply immediately. | D-23, D-27 |
| RV-04 | `SubscriptionPlan` CRUD: `code` (unique, immutable), `name`, `status` (`ACTIVE`,`RETIRED`), `billing_cycle_options` ⊆ {`MONTHLY`,`QUARTERLY`,`YEARLY`}, `self_service` (whether users may pick it in the app), entitlements. Endpoints `GET/POST /api/v1/platform/subscription-plans`, `GET/PUT …/{id}`, `POST …/{id}/retire`. No plan seeded. Edits apply immediately to subscribers (D-20). Retired plans cannot be newly granted; retire is one-way. | §149.15, D-08, D-17, D-20 |
| RV-05 | User subscription: `id`, `subject_type` (`USER` now), `subject_id` (user id), `plan_id`, `status` (`ACTIVE` now), `billing_cycle`, `granted_by` (`ADMIN`/`SELF_SERVICE`), `started_at`. At most one active subscription per user. Covers all the user's buildings. | D-22, D-25 |
| RV-06 | Admin grant: `POST /api/v1/platform/users/{userId}/subscription` (plan + cycle). Self-subscribe: `POST /api/v1/me/subscription` — only `self_service` plans; free for now. Errors: plan missing/retired/not self-service, cycle not offered, already subscribed → explicit codes. | D-25 |
| RV-07 | `GET /api/v1/me/entitlements` → free tier merged with the caller's own active plan (each user for themselves, D-22). `GET /api/v1/platform/users/{userId}/subscription` for admins. | D-22, D-23 |
| RV-08 | Fee schedule (data): named one-time fees, first `BUILDING_CREATION`: `amount`, `currency`, `required` (bool). Admin can view/edit; audited. No amount is hardcoded. | D-24, D-27 |
| RV-09 | Payment record: admin records a fee payment against a reference (`BUILDING_APPLICATION`, application id): amount, currency, method `MANUAL`, external reference, paid date, recorder. `GET` by reference answers "is the fee settled?" (settled if a required fee has a payment ≥ amount, or the fee is not required). Collection method behind a port (gateway later). | D-24, D-26 |
| RV-10 | Roles: platform endpoints need `SUPER_ADMIN`, `PLATFORM_ADMIN` or `SUBSCRIPTION_ADMIN`; `/me/*` needs any logged-in user. | D-08 |
| RV-11 | Every mutation audited (actor, action, before/after, time). Kafka events deferred (D-13 pattern). Subscription never grants access to data a user is otherwise not allowed to see. | §149.21–22 |

## Out of scope (this slice)

Payment gateway / real charging, prices on plans, trials, renewals/expiry, cancel/suspend of
subscriptions, building-level subscriptions (supported by `subject_type` later), enforcing
entitlements inside other services, back-office and app UI, Kafka events.

## BRD deviations (operator decisions)

D-22 user-level subscriptions (BRD: building-level); D-24/D-26 one-time creation fee replaces
approval-time subscription attach (BRD §149.7 step 6).
