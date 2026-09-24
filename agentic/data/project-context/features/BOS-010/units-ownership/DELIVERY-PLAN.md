# F4 delivery proposal

Status: APPROVED sequence, 2026-09-24. Split into bounded executable task records
before each implementation step; no change to accepted scope.
Classification: FEATURE_WITH_TASKS / NO_REPLAN, continuing BOS-010's existing sequence.

| Order | Category / outcome | Dependencies and verification |
|---|---|---|
| F4-T1 | DB/Integration — settle and implement the single membership/invitation authority, preserve F2 initial admins, phone-linked identity/claim and authoritative access checks | Approved ADR and permission policy; migration/retry/revocation/object-isolation tests |
| F4-T2 | BE — building selector/read APIs, floors and individual unit creation/editing/list/detail | T1; uniqueness/normalization and cross-building tests; update activation prerequisites |
| F4-T3 | DB/Integration — unit bulk generation and CSV/Excel preview/commit | T2; same validator, concurrency, bounded parsing, batch rollback |
| F4-T4 | BE — assignment/co-ownership, partial/full transfer and protected history | T1–T2; temporal/share policy, lock/concurrency and same-day/dated cases |
| F4-T5 | DB/Integration — ownership audit/outbox publisher and transfer documents | Transactional audit/outbox begins WITH T4's writes, never as an unaudited interim implementation; storage authorization and outage/retry evidence |
| F4-T6 | BE — gateway/OpenAPI/event contracts and approved revenue-policy enforcement | Contracts drafted before dependent implementation; gateway/direct-service permission parity |
| F4-T7 | Mobile — selector/portfolio, invitations, units and batch flows | T1–T3/T6; localized validation and explicit access states |
| F4-T8 | Mobile — assignment/transfer confirmation, documents and history | T4–T6; state/error handling, owner-filtered views |
| F4-T9 | QA — scoped regression, end-to-end flow, device verification and readiness evidence | All; backend/Flutter architecture checks unchanged; manual acceptance/release approval |

Split each row further if it cannot finish within the harness task budget. Run
implementation tasks individually, with one task-finish commit per bounded task.
No new agent delegation or parallel agents is required by this plan.

## Required design package

Prepared conditionally for review: [feature HLD](ARCHITECTURE.md), project HLD draft,
[membership ADR](adr/ADR-F4-001-membership-authority.md), and [LLD](TECH-SPEC.md) covering
API/schema/security/migration/test design. [APPROVAL.md](APPROVAL.md) consolidates the
proposed branch. After product choices and technical approval, turn this sequence into
bounded executable task descriptions with source requirement IDs.

## Readiness

The initial requirements task is producing reviewable proposals only. Do not start
implementation, migrate databases, grant user access, or send invitations until the
required product rules and technical approval are recorded.


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
