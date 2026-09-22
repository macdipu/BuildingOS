# BOS-002 draft delivery sequence

Status: proposal only; no implementation task or sprint commitment is approved.
Classification: provisional STORY_TASK. Sprint handling: BACKLOG_ONLY. This is a
bounded vertical slice across existing modules; no new Epic or fleet of services.
Task-level estimates and executable task docs follow decisions and technical readiness.

| Order / story | User outcome and candidate engineering work | Dependencies | Acceptance |
|---|---|---|---|
| S1 — Sign in safely | BE: provider-proof verifier, local identity mapping, session contract/rotation/logout. Mobile: replaceable PhoneOtpProvider with initial backend adapter/DevelopmentOtpProvider (`000000`) and phone-number/OTP flows, envelope adapter, storage error propagation, redacted HTTP logs, bounded concurrent refresh. QA: provider assertions, replay/expiry/rate controls and refresh-failure regression. | OTP-PROVIDER.md; D-03; reconcile Flutter toolchain baseline before mobile work | ID-01–03,UI-01 |
| S2 — Enter an authorized building | BE/DB: seed-script SUPER_ADMIN bootstrap (D-02, resolved), phone-based invitations/membership/permissions and building-context checks; gateway routes/CORS matched to the contract. Mobile: selector, no-membership/denied states. QA: cross-building and revoked-access tests. | S1; D-04 | ID-04–05,BL-01 |
| S3 — Set up buildings and units | BE/DB: building creation/first-admin consistency and recovery; unit CRUD/validation/migrations. Mobile: authorized create/edit/list/detail and owner-filtered results. QA: concurrent duplicate number, permission and foreign-building object-ID tests. | S2; D-06; audit/outbox with first writes | BL-02,UN-01–02,EV-01 |
| S4 — Assign and transfer ownership | BE/DB: temporal/share invariants, concurrency and history; Mobile: assign/transfer confirmation/history; QA: overlapping shares, co-owner/partial transfer cases after policy selection. Emit versioned events in the write transaction. | S3; D-04,D-05 | OW-01–02,EV-01 |
| S5 — Verify the whole slice | QA/Mobile/BE: sign-in-to-unit/ownership flow, security/contract/migration tests, Kafka outage/replay, localization and on-device checks. Capture manual acceptance/UAT requirements and release evidence. | S1–S4; approved decisions; real provider sandbox availability | All BOS-002 requirements; scoped §§143A/E |

## Sequencing rules

1. Use the explicitly selected `000000` development OTP adapter and the resolved seed-script
   SUPER_ADMIN bootstrap (D-02); complete the remaining permission, session and ownership
   decisions (D-03–D-06) through explicit reviewable proposals.
2. Produce the SRS, interface contracts and focused architecture decisions. Keep external
   identity verification separate from building authorization. Decide how services learn
   membership changes without introducing a shared database or an authorization bypass.
3. Specify audit, outbox and consumer idempotency with the first relevant domain writes,
   not as a later cleanup story. Retry/DLQ tests must cover the proposed integration.
4. Obtain technical readiness and approval, then create bounded tasks under these stories.
   Existing BOS-001 approval does not approve BOS-002 implementation.
5. Verify/repair the recorded Flutter baseline as a scoped prerequisite; do not obscure
   new client failures behind the existing nonblocking baseline CI job.

## Scope edges to settle during design

Building/unit screens in the BRD include rental and financial fields owned by later
milestones. This slice presents only implemented domain data; later integrations must
have explicit contracts and cannot be represented by invented balances or occupancy.
Transfer document attachment depends on document storage/access policy and needs a
specific scope decision before implementing upload behavior. Ownership transfer emits
an event and preserves history; Reporting projections are delivered in BOS-008.

## Completion evidence

Each final task must link its requirement IDs, migration/contract impact, security and
failure tests, review result and remaining risks. BOS-002 needs fresh mobile/API
integration evidence; BOS-001's 37 tests remain foundation regression evidence only.
No estimated dates, staffing promises, live provider calls or production deployment
are part of this planning artifact.

## Provider layer and current login scope

Phone-number OTP is the working current scope; Google is deferred. The user selected fixed `000000` for development. Use the backend development adapter
without SMS or an external vendor SDK; a real vendor is a later adapter choice.
Add [provider replacement acceptance](OTP-PROVIDER.md) to S1/S5. Do not enable real SMS
or attach billing to satisfy a planning task. Live phone verification remains separate
acceptance once its account and cost constraints are configured.
