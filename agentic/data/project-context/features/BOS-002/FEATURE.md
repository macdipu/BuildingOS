# BOS-002 — Identity, building access, units and ownership

Status: planning requested; implementation not approved.
Date: 2026-09-22. Run: `RUN-AC70D215A0924BC3AE9B56B7FD4BCF1B`.
Source: [BRD](../../BuildingOS_BRD_Agentic_Development.md),
[BOS-001 backlog](../BOS-001/BACKLOG.md), and the user's explicit approval to
close BOS-001 and proceed to BOS-002 planning.

## Outcome

Deliver the first user-facing vertical slice: sign in with mobile-number OTP, select a
building the user may access, and view/manage units and effective-dated ownership
according to server-validated permissions. An owner may access their own units;
changing a building header or object ID must never confer access.

## Existing implementation

BOS-001 local foundation is completed and approved. Identity/building services expose
secured metadata only. The gateway, independent JWT enforcement, service databases,
Flyway, contracts, Kafka, observability, and verification are available. Flutter is an
existing GetX/Clean Architecture starter whose login and API format need adaptation.
See [BASELINE.md](BASELINE.md) for scoped source findings and verification limits.

## In scope for this plan

- Mobile-number OTP integration, sessions, refresh rotation and logout. Google sign-in
  is deferred under the explicit working interpretation of the latest user direction.
- A replaceable OTP provider layer: DevelopmentOtpProvider accepting `000000` locally
  initially; application identity and business data remain provider-independent.
- Initial administrator provisioning and authorized building membership lifecycle.
- Building selection with server-enforced membership and object authorization.
- Building/unit setup, owner views, assignment/transfer and preserved ownership history.
- Audit with privileged mutations; transactional outbox with each event-producing write;
  idempotent consumers for any cross-service projections introduced in this slice.
- Flutter flows from BRD §§39–42,48–54, adapted to the delivered slice, with English/Bangla
  localization, existing design tokens, and safe storage/logging/session behavior.
- Source-mapped acceptance criteria and a dependency-ordered implementation proposal.

## Outside this milestone's implementation scope

Rental/tenant/lease implementation (BOS-003), financial transactions (BOS-004),
offline financial sync (BOS-005), reporting dashboards (BOS-008), production deployment,
and all twelve proposed deployables. Ownership transfer must not mutate a lease;
the full lease-preservation integration scenario will be completed when Rental exists.
No fake financial/dashboard values or login bypass is part of this plan.

The BRD (revised 2026-09-22) now defines a SaaS back-office/subscription/onboarding
domain as CORE PRODUCT SCOPE (§4 PLATFORM_ADMIN/ONBOARDING_AGENT/SUPPORT_AGENT/
SUBSCRIPTION_ADMIN roles, §8.12 Subscription Service, §110.2–110.3 building
lifecycle/subscription, §149 full spec, Phase 1B). This is out of scope for BOS-002:
BOS-002 keeps its original identity/building/units/ownership slice for the existing
`user_app` mobile client, with buildings created directly (no application/approval
state machine, no `buildingos_backoffice_web`, no subscription/entitlement
enforcement). The back-office/SaaS domain needs its own FEATURE.md/intake pass before
any implementation; do not fold it into BOS-002.

## Decisions and authorization

The BRD specifies SUPER_ADMIN can create buildings/manage building admins and
BUILDING_ADMIN can configure a building/manage units/owners. Q-02/D-02 (how the first
SUPER_ADMIN is established and how membership invitations/grants work) is now RESOLVED:
a deploy-time seed script/config provisions the first SUPER_ADMIN (seed phone
`01306999005`), which logs in like any user via phone + OTP — no admin panel or
password exists in this design. Building admins invite users by phone; invited users
activate membership via normal phone-OTP verification, separate from ownership
assignment. See [DECISIONS.md](DECISIONS.md).
The user delegated a no-cost starting provider and required vendor replaceability.
[OTP-PROVIDER.md](OTP-PROVIDER.md) records the explicit `000000` development decision, provider boundary and future live-SMS
cost boundary. Session, object-permission and ownership edge cases (D-03–D-06) must
still be resolved before their dependent implementation. Approval to plan is not
technical or implementation approval.

## Planning classification

Provisional STORY_TASK: one bounded user journey with five related outcomes across
existing identity/building/gateway/Flutter modules. No extra Epic adds useful ownership
or traceability. BACKLOG_ONLY: no sprint dates/capacity commitments or code execution
have been requested. Re-evaluate classification after requirement decisions; keep
implementation proposals explicitly unapproved until technical readiness.
