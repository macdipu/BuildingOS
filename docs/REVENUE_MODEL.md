# Revenue model (BOS-010 F5a)

Owned by `subscription-service` (`subscription_db`). Decisions: BOS-010 DECISIONS.md D-22..D-27.
This deliberately deviates from the BRD's building-level subscription (§110.3, §149.15).

## Current model

- **Per-user subscriptions.** One active subscription per user covers every building the user
  belongs to. Each user unlocks paid features only for themselves.
- **Free tier.** Every user gets the free-tier entitlements (initially `maintenance.enabled`).
- **Plans.** Operator-defined (`/api/v1/platform/subscription-plans`); none are seeded. Edits
  apply immediately to subscribers; retiring is one-way and only blocks new grants.
- **Granting.** Back-office admin assigns a plan (`POST /api/v1/platform/users/{userId}/subscription`)
  or the user self-subscribes to a `selfService` plan (`POST /api/v1/me/subscription`). No charge yet.
- **One-time building-creation fee.** Amount/currency/required are set via
  `/api/v1/platform/fees/BUILDING_CREATION`; payments are recorded manually by an admin; status
  `SETTLED | UNPAID | NOT_REQUIRED` gates building-application approval. An unconfigured fee
  returns `409 FEE_NOT_CONFIGURED` (never treated as free).
- **Who may manage:** platform roles `SUPER_ADMIN`, `PLATFORM_ADMIN`, `SUBSCRIPTION_ADMIN`.
- **Entitlement read:** `GET /api/v1/me/entitlements` = free tier layered with the caller's plan
  (flags OR-ed; plan limits/text override free-tier values).

## Changing the model

| Change | How |
|---|---|
| Which features are free | `PUT /api/v1/platform/free-tier` (data) |
| Plan contents | Plan APIs (data) |
| Fee amount / fee on-off | `PUT /api/v1/platform/fees/BUILDING_CREATION` (data) |
| New feature key | Add a `Feature` enum constant (`catalog/domain/model/Feature.java`) |
| New one-time fee | Add a `FeeCode` (and `ReferenceType` if needed); operator sets its schedule |
| Building- or organization-level subscriptions | Add a `SubjectType` value and an `EntitlementResolver` implementation; `subscription` already stores `subject_type`/`subject_id` |
| Real payment gateway | Add a `PaymentMethod` value and an adapter that records payments |
| Trials, renewals, cancellation, prices | Not built yet (later slices) |

Other services do not enforce entitlements yet; they will read them from subscription-service.
