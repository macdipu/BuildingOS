# BOS-010 F4 — Units and ownership

Status: REQUIREMENTS_READY; F4 proposal and technical design approved 2026-09-24.
Run: `RUN-0BF1AD2443404B03BE7350F0C91BD0C0`. Parent: [BOS-010](../FEATURE.md).

## Request and objective

Source request: operator "continue" on 2026-09-24 after F2 local/test completion and
the stated next slice "units and ownership". Deliver G4 of the parent requirements
against the implemented building lifecycle, not the superseded BOS-002 direct-building model.

## Scope

- Authorized building selection and a cross-building owned-property portfolio.
- Building-scoped unit creation/editing/list/detail, the six defined unit types,
  uniqueness, positive area, floors, bulk generation/import per BRD §149.9.
- Assignment, co-ownership and partial/full transfer with preserved effective-date
  history and transactional aggregate-share validation.
- Global user reuse, explicit separation of ownership and membership, and the
  invitation/membership dependencies needed for an owner to enter a building.
- Add the >=1-valid-unit activation prerequisite promised by BOS-010 D-30.
- Backend, API/gateway contracts, localized Flutter flows, authorization and
  concurrency checks, and auditable domain writes. Ownership transfer events and
  document handling need explicit implementation boundaries during design.

## Existing constraints and acceptance

Use Clean Architecture, feature-first, one use case per action and isolated service
databases. Reuse auth-service identity, building-service canonical Building and
membership records, existing S3 document-storage boundary, and current Flutter conventions.

A duplicate unit number cannot be created concurrently within one building. A user
cannot read or mutate another building's protected objects by changing request IDs.
Ownership never substitutes for a membership grant. Total shares never exceed 100%
for an effective period; transfers preserve prior ownership and never change leases.
One global user can own several units in several buildings. Activation must reject
a building with no valid unit once this slice lands. Mobile screens show actual
implemented data and explicit empty/denied/error states.

## Exclusions

Rental/leases/tenants, financial balances/payments/reporting, live SMS, general
support/assisted-access grants, the separate back-office web app, production deployment
and the full guided-onboarding wizard remain their own slices. Future financial
and rental data must not be fabricated in unit/portfolio screens.

## Classification and sequence

Brownfield: new unit/ownership aggregates extend existing building lifecycle,
membership persistence, identity and contracts. Minimum useful hierarchy: one
feature with bounded BE, DB/Integration, Mobile and QA tasks. NO_REPLAN reuses the
parent's feature sequence; no sprint dates, staffing commitments or new Epic.
Each implementation step needs a separate governed task; avoid the multi-hour F2 task.

## Decisions to resolve

- Exact permissions, revocation/history visibility and suspended-building behavior.
- Ownership date granularity/boundaries, partial transfer, correction and precision.
- Unit-number normalization, floor representation, area units and precision.
- Membership/invitation source of truth and account-linking behavior.
- Which per-user entitlements/limits govern foundational unit setup (D-22..D-27).
- Ownership-transfer event/outbox scope and transfer-document policy.
- Bulk import atomicity and review/validation UX.

Four product questions (permissions, dates, units/precision, and revenue treatment) were presented to the operator on 2026-09-24; see DECISIONS.md. Suggestions
are proposals, not accepted business rules; no prior F2 approval covers this slice.

## Sources

- [Parent requirements](../REQUIREMENTS.md), G3 ON-02 / G4.
- [Parent decisions](../DECISIONS.md), D-22..D-30.
- [BRD](../../../BuildingOS_BRD_Agentic_Development.md), §§8.1–8.2, 19.1,
  42, 48–52, 110.1, 127, 143A/E, 149.9, 149.13–149.14.
- [Carried ownership decisions](../../BOS-002/DECISIONS.md), D-04..D-06,
  rechecked against the revised BRD rather than silently reused.
- [F2 release](../building-application/RELEASE.md).

## Current review package

Intake/context are complete. Requirements remain blocked on product decisions.
[APPROVAL.md](APPROVAL.md) consolidates the proposed defaults and conditional
[HLD](ARCHITECTURE.md), [LLD](TECH-SPEC.md) and membership ADR. Preparing these artifacts
does not advance the run past REQUIREMENTS or authorize implementation.


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
