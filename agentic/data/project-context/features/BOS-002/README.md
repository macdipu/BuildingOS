# BOS-002 — Planning in progress

BOS-001 local foundation is [completed and approved](../BOS-001/RELEASE-READINESS.md).
This work item plans the first user-facing slice: mobile-number OTP sign-in → authorized
building selection → units and ownership. No BOS-002 application code has been changed.

- [Feature and scope](FEATURE.md)
- [Scoped baseline and client integration risks](BASELINE.md)
- [Requirements and acceptance draft](REQUIREMENTS.md)
- [Decisions requiring resolution](DECISIONS.md)
- [Draft delivery sequence](DELIVERY-PLAN.md)
- [OTP provider choice and replacement layer](OTP-PROVIDER.md)

Current gate: REQUIREMENTS. The user requested mobile only, a free initial OTP choice
and vendor replaceability. The follow-up explicitly selected `000000` for development. A local/test-only
DevelopmentOtpProvider sits behind an adapter, with no SMS/vendor setup needed now. Working interpretation: phone-number login only; Google
deferred. Real SMS requires a separately configured billing-enabled provider.
First-admin/building access provisioning (D-02/Q-02) is now RESOLVED: first SUPER_ADMIN
is provisioned by a deploy-time seed script/config (seed phone `01306999005`), logging
in the same way as any user (phone + OTP, dev code `000000` locally) — no admin panel
and no password exist. Building admins invite users by phone; the invited user activates
membership by completing normal phone-OTP verification; ownership assignment is a
separate action from membership grant. See [DECISIONS.md](DECISIONS.md).
Other session, permission and ownership details (D-03–D-06) are explicit pending design
inputs, not inferred rules. The provisional sequence is STORY_TASK/BACKLOG_ONLY; it
creates no sprint or implementation approval.

The BRD was subsequently revised (2026-09-22) to make a SaaS back-office/subscription/
onboarding domain CORE PRODUCT SCOPE (§149; platform roles in §4; Subscription Service
in §8.12; building application/lifecycle in §110.2–110.3). It remains out of scope for
BOS-002 — a separate work item, not yet planned/intake'd. BOS-002 still creates
buildings directly (no application/approval workflow, no back-office web app, no
subscription enforcement) for the existing mobile-only slice.

Run: `RUN-AC70D215A0924BC3AE9B56B7FD4BCF1B`. Resume this run to move past REQUIREMENTS.
Next: prepare the SRS and focused technical proposals for D-03–D-06, then review
requirements/architecture and break the accepted scope into executable tasks.
