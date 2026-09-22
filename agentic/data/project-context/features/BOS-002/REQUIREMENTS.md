# BOS-002 requirements draft

Status: DRAFT — not ready for technical approval or implementation.
Source: [BRD](../../BuildingOS_BRD_Agentic_Development.md). Requirement identifiers
below are local to BOS-002. Open choices are in [DECISIONS.md](DECISIONS.md).

## User journey

Logged out → mobile-number OTP login → verify identity → resolve server-authorized building
memberships → select a building → list/detail of permitted units → authorized owner
assignment or transfer → preserved history. Expired/revoked sessions, no membership,
forbidden objects and failed writes must have explicit error states. A missing
membership never falls back to an arbitrary building or dashboard.

## Requirements and observable acceptance

| ID | Requirement and acceptance | BRD source | Pending decisions |
|---|---|---|---|
| ID-01 | Phone login includes country code and six-digit OTP through a replaceable provider interface; verify, resend and change-number flows. Reject invalid/expired/reused verification and enforce the agreed expiry/retry/rate limits. Support +880. | §§40–41,93,109 | OTP-PROVIDER.md; live-SMS setup and provider-supported limits |
| ID-02 | Deferred: Google sign-in under the current phone-only interpretation. Keep future identity providers behind the same adapter boundary; do not ship Google UI/SDK work in this iteration. | User direction 2026-09-22 supersedes current slice of §40 | Scope interpretation awaiting optional clarification |
| ID-03 | Short-lived access tokens, refresh rotation and logout/session expiry. Reject a superseded refresh token under the selected reuse policy. Concurrent refresh success/failure completes every waiting request without unbounded retry. No credentials/OTP/token in logs, including debug HTTP output. | §93; BASELINE.md | D-03 |
| ID-04 | Protected routes validate authentication independently at gateway/service. Validate active membership, requested building and object permission on every business request; spoofed building headers/IDs and revoked membership do not grant access. | §§5,93,110,138 | D-04 |
| ID-05 | First SUPER_ADMIN provisioning is explicit and auditable: a deploy-time seed script/config creates the account (seed phone `01306999005`, D-02 resolved), which authenticates via the same phone+OTP flow as any user — no password/admin panel. Building admins invite users by phone to grant membership (separate from ownership assignment); BUILDING_ADMIN manages permitted building users/owners. A login alone never grants an administrative role. | §§4.1,8.1,93 | D-04 |
| BL-01 | Selector lists only accessible buildings, with name/address and the user's roles; selection establishes the authorized building context. Add Building is authorization-dependent; optional Join Building is not assumed. | §§5,42,110 | D-04,D-06 |
| BL-02 | Create/configure a building through the roles permitted by the BRD. Building creation and the first admin membership (seed script, D-02 resolved) have an explicit cross-service consistency/recovery plan; no orphan grants or unaudited privilege assignment. | §§4.1,8.1–8.2,15,17,93 | D-06 |
| UN-01 | Unit creation/editing validates unit number uniqueness within its building, positive area and allowed type (FLAT/PARKING/STORAGE/COMMERCIAL/COMMON/OTHER). Fields and optionality follow §49. Concurrent duplicate creation cannot bypass uniqueness. | §§8.2,49,110 | D-06 |
| UN-02 | Unit list/detail filters by membership and object permission. An OWNER with three owned units sees those units only and cannot retrieve another owner's private data by changing an ID. Unimplemented rental/financial projections are not fabricated. | §§48–50,138,143A | D-04; later BOS-003/004/008 data |
| OW-01 | Assign ownership with owner, share and effective date; history is retained. Aggregate active shares must not exceed 100%; overlapping/concurrent operations enforce that invariant transactionally. | §§19.1,51 | D-04,D-05 |
| OW-02 | Transfer requires confirmation, closes the appropriate old period and creates the new period while preserving history. It must not mutate a tenant lease. Partial, future/backdated and invalid-date cases follow the selected temporal policy. | §§52,127,143E | D-05; full lease integration in BOS-003 |
| EV-01 | Privileged/domain writes produce audit evidence alongside the mutation. Event-producing services insert outbox records in the same local transaction; consumer duplicates do not duplicate effects. Kafka interruption and replay tests prove recovery. Do not use distributed DB transactions. | §§15–17,93,96; BOS-001 backlog | Technical design of publisher/consumer retry and projections |
| UI-01 | Reuse GetX/Clean Architecture and localization/theme conventions; adapt login/API payloads to the agreed BRD envelope. Secure storage errors and authentication failures lead to explicit states; no fake successful session. | §§18,36–42,109,147; BASELINE.md | D-01,D-03 plus Flutter baseline |

## Authorization facts versus pending policy

Confirmed: SUPER_ADMIN creates buildings and manages building admins; BUILDING_ADMIN
configures its building and manages units/owners; OWNER reads owned units; ACCOUNTANT
has no ownership/role administration. A user may hold different roles per building.

The BRD's example permission lists do not specify every mutation, delegation or
revocation case. D-04 must define the exact unit/ownership/membership matrix and
cross-building exceptions for platform operations. No broad wildcard access follows
from a role label or from the presence of an active-building header.

## Required verification layers

- Domain: temporal ownership/share constraints, uniqueness, permission decisions.
- Integration: migrations/rollback implications, isolated service stores, outbox atomicity,
  duplicate consumer delivery and stale/revoked membership handling.
- Contract/security: provider/session assertions, refresh rotation/reuse, API envelope,
  gateway and direct-service authorization, object-ID substitution and tenant boundaries.
- Mobile: secure storage failure, concurrent refresh failure, redacted logging, localized
  login/selector/unit flows and loading/empty/denied/error states.
- End-to-end: authorized sign-in → building → units/ownership. Treat external-provider
  sandbox evidence and local fixture evidence separately. No real SMS is sent by planning.

Financial acceptance is deferred to its milestones. For §143E, BOS-002 proves its own
writes preserve ownership history and emit the transfer event without lease writes;
BOS-003 later verifies the complete scenario against an actual existing lease.

## Updated direction — 2026-09-22

The user requested mobile only, a free initial OTP option, and a vendor-switching layer.
Working interpretation: mobile-number OTP only, Google deferred. See
[OTP-PROVIDER.md](OTP-PROVIDER.md) for the selected local/test `DevelopmentOtpProvider` accepting `000000`,
future live-SMS billing boundary, app/backend interfaces and replacement tests. This
resolves the request for an initial development provider, not unlimited free SMS.
No provider billing or external enrollment has been authorized or performed.

The follow-up "for developmet use all 0," explicitly sets the development OTP to
`000000`. No vendor account or SMS is needed now. This replaces the earlier Firebase
starting-adapter proposal. Challenge checks and normal authorization remain in force;
non-local environments reject the development provider. Implementation is still pending.

## Resolved — 2026-09-22 (D-02)

First SUPER_ADMIN: deploy-time seed script/config, seed phone `01306999005`; no admin
panel, no password anywhere in this design; the seeded account authenticates via the
normal phone+OTP flow. Membership grant: an admin invites a user by phone number; the
invited user activates membership by completing normal phone-OTP verification.
Ownership assignment is a separate action from membership grant. See DECISIONS.md.
D-03–D-06 remain open technical/UX inputs.

The BRD was later revised (2026-09-22) to make the SaaS back-office/subscription/
onboarding domain CORE PRODUCT SCOPE (§149, Phase 1B). It stays out of scope for
BOS-002, which still creates buildings directly for the mobile-only slice; that domain
needs its own FEATURE.md/intake before implementation.
