# BOS-010 F4 — Units and ownership requirements

Status: REQUIREMENTS_READY, operator-approved 2026-09-24. Business choices in
[DECISIONS.md](DECISIONS.md) are resolved by the approval record.
Run: `RUN-0BF1AD2443404B03BE7350F0C91BD0C0`. Baseline: [BASELINE.md](BASELINE.md).

## Actors and journeys

Building Admin: select an authorized building → define floors/units individually or
by reviewed batch → separately invite/link owners → allocate shares → confirm a
transfer → review preserved history. Setup supports ONBOARDING before activation.

Owner: phone OTP → claim an intended invitation → My Buildings / My Properties →
own unit → own ownership/history. Owning multiple units/buildings reuses one identity.
The UI never selects an arbitrary building when access is absent or revoked.

Platform Admin / Super Admin: use explicitly privileged, audited routes to administer
the approved scope. Onboarding/support agents, accountants and committee members do
not gain ownership-edit authority merely from a role label.

## Traceable requirements and acceptance

| ID | Requirement / observable acceptance | Source and dependency |
|---|---|---|
| UO-01 | My Buildings lists authorized buildings with name/address/lifecycle/roles and owned-unit count. Every detail request rechecks current authorization. Empty, forbidden and revoked states are explicit; supplied building IDs/headers never grant access. | BOS-010 BL-01; BRD §§42,149.14; UO-D01 |
| UO-02 | Floor/unit setup supports FLAT, PARKING, STORAGE, COMMERCIAL, COMMON and OTHER. Unit number is unique per building under the chosen normalization; area is positive. Floor, optional bedrooms, maintenance-rate metadata and notes follow §49. Unsupported financial calculations are absent. | BOS-010 UN-01; BRD §49; UO-D03 |
| UO-03 | Concurrent creates/renames for the same normalized number in one building produce one success and one explicit conflict; the same number in different buildings is valid. Invalid type, area, precision or references never partially persist. | UN-01; UO-D03 |
| UO-04 | Individual floors, basement/ground/roof/common representation, range generation, floor/count/pattern generation, duplicate-floor layout and CSV/Excel preview/import reuse the same unit validation. Show row errors before confirmation; final validation rechecks conflicts. | BRD §149.9 steps 2–3; batch policy pending |
| UO-05 | A global user may own multiple units in one or many buildings; co-owners are supported. Owner identity, building membership and ownership allocation are separate records/actions. Inviting/claiming does not silently assign ownership; assigning ownership does not silently grant membership. | OW-01; BRD §§19.1,149.9,149.13 |
| UO-06 | Assignment records owner, share, effective time/date and optional notes. Share >0; total effective allocations <=100%, including concurrent requests and pending reservations if enabled. Partial ownership below 100% is allowed; 100% is not forced. | OW-01; §51; UO-D02/03 |
| UO-07 | Confirmed transfer moves a specified share from one owner to another, records actor/reason/reference/document as applicable, closes the affected prior allocation period and preserves historical shares. A transfer cannot move more than the source owns or change a lease. | OW-02; §§52,127,143E; UO-D02 |
| UO-08 | Owner-filtered unit/history reads and My Properties show real allocations only. No contact leakage through owner filters, history, transfer documents or changing object IDs. Former/revoked-member visibility follows the explicitly selected matrix. | BOS-002 UN-02; §§143A,149.14; UO-D01 + history policy |
| UO-09 | Add D-30's valid-unit prerequisite alongside active Building Admin before activation. The check and conflicting unit mutations cannot race to activate an empty building. Migration/reactivation behavior follows the selected policy. | ON-02; D-30; status policy |
| UO-10 | Privileged writes have durable actor/action/time/reason/change evidence in their local transaction. Ownership transfer writes a versioned outbox event atomically; Kafka outage/retry does not lose committed events. | §§52,93,127,149.21; D-13 applies only to its documented scope |
| UO-11 | Backend enforces the selected per-user entitlement policy; UI labels reflect actual access. A paid user's plan never unlocks another user's features or substitutes for building authorization. Missing dependencies fail explicitly without partially changing ownership. | D-22..D-27; UO-D04 |
| UO-12 | Flutter implements selector/portfolio, units, assignment/transfer, history and invitation states with English/Bangla strings, clear validation, explicit transfer confirmation and loading/empty/denied/error states. No invented rent/occupancy/due summaries. | UI-01; §§42,48–52,149.14 |
| UO-13 | Transfer uploads use the existing content-type/size safety boundary with ownership-scoped metadata/access. Upload/list/download/remove behavior and requiredness are specified before implementation; application documents never become generally visible to building members. | §52; D-28; document policy |

## Concrete acceptance examples

These are proposed test cases; pending policies are identified rather than treated as approved.

- Under UO-D03, create "4a" in Building A, then " 4A " → conflict. Create "4A" in B
  → allowed when the caller is authorized for B.
- Allocate A=60%, B=40%. A transfers 20 percentage points to C → A=40%, B=40%, C=20%.
  The prior A=60% period and transfer are retained. Another transfer of 50 from A
  fails with no ownership/audit-event partial write.
- Start with 90% allocated. Race two new 10% allocations → one may succeed;
  committed total never exceeds 100%.
- An owner of three units sees only those authorized units; selecting a fourth unit's
  ID, history URL or document ID does not expose its details.
- A membership revoked before a request is authorized must fail that request.
  An existing JWT alone cannot preserve access after revocation.
- If UO-D02 selects immediate changes, past/future requests are rejected; repeated
  same-day transfers retain ordered history and the correct current owner.
- If dated ownership is selected instead, validate every overlapping effective interval,
  not merely today's share total, including concurrent scheduled transfers.
- No valid unit → activation rejected; create one valid unit → activation may succeed
  when other prerequisites pass. A rollback of unit creation cannot satisfy activation.
- Kafka unavailable → the committed transfer remains durable with a pending outbox
  record; recovery republishes it with the same event ID.
- Bulk preview contains one duplicate → show that row's error; no silent skipped rows
  under the proposed all-or-nothing policy.

## Boundaries and unresolved readiness

Rental/finance/reporting/support/back-office web remain separate. Full guided onboarding
is separate, but its floor/unit/owner prerequisites in this slice are preserved.

UO-D01..04 and supplementary behavior are approved. The immediate-date/free-core
branch and building-service membership authority are selected. The operator also
approved the technical package; record the harness technical gate against finalized
evidence before coding. Release approval remains separate.


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
