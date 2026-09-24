# F4 decision proposal

Status: RESOLVED / APPROVED by operator, 2026-09-24.
Source: operator requested "continue" on 2026-09-24. This authorizes preparation, not
answers to the unresolved questions below. F2 release approval remains limited to F2.

## Product decisions presented to the operator

| ID | Proposed rule | Source of uncertainty | Status |
|---|---|---|---|
| UO-D01 | Building Admins manage units/ownership in assigned buildings; owners read only their own units and ownership history; Super/Platform Admins have audited cross-building administration. Other roles receive no ownership-edit authority. | BOS-002 D-04 / BOS-010 OW-01 leaves the full matrix open. | APPROVED 2026-09-24 |
| UO-D02 | First release assignments/transfers take effect immediately, on today's Asia/Dhaka calendar date; reject past/future effective dates. Preserve partial/co-owner transfers and every historical state. Full dated corrections/scheduling require a later approved policy. | BOS-002 D-05 / BOS-010 OW-02 leaves temporal semantics open. Alternative: full past/future timeline validation now. | APPROVED 2026-09-24 |
| UO-D03 | Unit area in square feet, 2 decimal places; shares to 4 decimal places. Trim unit-number edges and compare case-insensitively within a building; retain display spelling. | BOS-002 D-06 leaves units/precision/normalization open. Alternative: square metres. | APPROVED 2026-09-24 |
| UO-D04 | Building selection, unit/ownership setup and invitations are free foundations for maintenance. Defer max_units/max_users enforcement until counting/subject rules are specified; paid rental/reporting permissions remain per user. | D-23 says only maintenance is initially free; the catalog lacks unit/ownership keys and F5a deferred consumer enforcement. This proposal changes the interpretation and needs explicit confirmation. | APPROVED 2026-09-24 |

Choosing the dated alternative to UO-D02 requires valid-from inclusive/valid-to exclusive
date boundaries, timezone, correction authority, overlapping future transactions and
retroactive access rules to be included in technical review. No silent fallback to a
today-only implementation is allowed.

## Additional policy details to review with the proposal

These defaults were approved with the full F4 review packet; they are new policy,
not facts inferred from existing code.

- **Membership versus ownership:** invite by phone as a separate, explicit operation;
  the recipient claims it through an authenticated, phone-verified account. Assign
  ownership only to a linked global user; do not fabricate duplicate user accounts.
  For the concrete first implementation, wait for invitation claim/link before
  allocating ownership; no pre-claim share reservation. Pending-owner allocations
  remain an alternative requiring a revised model if selected.
- **History visibility:** a current owner may read their own share/transfer records,
  but not unrelated owners' contact data. A former owner with active membership
  retains access to their own historical records, without current-unit administration.
  Revoked membership removes building data access; retained legal ownership/history
  is not deleted. Whether historical statements must remain accessible after revocation
  needs a separate exception if required.
- **Building status:** allow admin unit/owner setup in ONBOARDING and ACTIVE.
  SUSPENDED buildings are read-only for this slice; suspension/reactivation remain
  platform actions. New activation and reactivation require a valid unit and active
  admin. Existing ACTIVE buildings with zero units are not automatically demoted.
- **Bulk operations:** show editable preview and per-row errors before confirmation.
  A confirmed batch is all-or-nothing; a conflicting row rolls back the batch.
  CSV and Excel plus floor-pattern generation remain in scope. Import limits and
  upload parsing bounds are technical safeguards, not subscription limits.
- **Transfer documents:** retain the BRD transfer reference/document field. Reuse the
  storage port with separate ownership-specific metadata and permissions. Proposed:
  document/reference optional, audited when supplied; their business requiredness is
  not specified by the BRD.
- **Owner corrections:** no arbitrary delete/overwrite of an ownership record.
  Today's correction would be a new, reasoned compensating transfer. More permissive
  historical corrections require an explicit business policy.

## Engineering choices to resolve at TECHNICAL

- **One membership authority:** prefer bringing the final ownership of membership
  and invitations into auth-service as BRD §8.1 specifies. F2's initial memberships
  must migrate with an explicit compatibility/rollback path, while building-service
  consumes a permission contract. Compare this against a documented interim
  building-service authority; never maintain two independent sources of truth.
  This is a significant boundary decision and warrants an ADR and technical approval.
- **Identity provisioning:** never relax /internal/users/provision to accept all
  authenticated callers. Phone-based invitation/linking must prove building authority
  and bind the eventual subject to the verified phone.
- **Transaction boundaries:** lock the target unit for allocation/transfer; use a unique
  normalized (building_id,unit_number) key for concurrent number uniqueness. Keep
  authorization/revocation and the write's commit ordering explicit.
- **Dates:** if UO-D02 chooses immediate changes, record server timestamps and ordered
  ownership revisions as well as the displayed local date, preserving same-day
  repeated transfers. Never create ambiguous zero-length date-only ownership history.
- **Events:** preserve ownership.transferred through a transactional outbox and
  retryable publisher; consumer idempotency belongs to later consuming services.
  D-13 did not waive the ownership event. Include the versioned event contract now.
- **No live side effects during preparation:** no SMS, deployment, database migration,
  live user invitation or business data mutation occurs in this requirements task.

## Consolidated review

[APPROVAL.md](APPROVAL.md) combines these proposed policies with the HLD, LLD and
membership ADR. Accepted by the operator's explicit "approve" on 2026-09-24.


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
