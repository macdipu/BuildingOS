# F4 review and approval packet

Status: APPROVED by operator, 2026-09-24; see decision record below.
Run: `RUN-0BF1AD2443404B03BE7350F0C91BD0C0`. Prepared 2026-09-24.

## Proposed product defaults

1. Building Admins manage their own buildings' units and ownership. Owners read their
   own records. Super/Platform Admins have audited cross-building administration.
2. Assignments/transfers take effect immediately on today's Asia/Dhaka date. No
   backdated/future-dated ownership in this first implementation.
3. Square feet with 2 decimal places; shares with 4 decimal places; trimmed,
   case-insensitive unit numbers unique within each building.
4. Core building selection, units, ownership and invitations are free prerequisites
   for maintenance. Rental/reporting remain per-user paid features. max_units/max_users
   enforcement waits for an explicit counting policy.

These are accepted UO-D01..04. Earlier "continue" messages were not used as approval;
the subsequent explicit "approve" is the authorization.

## Other behavior included in this proposal

- Invite OWNER by phone; claim in-app with the verified phone/global account.
  No SMS or ownership allocation before the owner links/claims membership.
- Assign/transfer only to linked members. Membership and ownership remain separate.
- Current members can read their own historical ownership after transfer; revoked
  members lose building access without deleting their legal ownership/history.
- ONBOARDING/ACTIVE permit setup; SUSPENDED is read-only for these flows.
  Activation/reactivation require an active admin and a valid unit; existing ACTIVE
  buildings are not automatically demoted.
- Batch generation/import uses editable preview, then all-or-nothing commit.
- Transfer reference/document are optional; document access is limited to authorized
  admins and relevant transfer parties with current membership.
- No arbitrary ownership deletion/overwrite; corrections use a new audited action.

## Proposed architecture

Extend the existing building-service membership table as the single authority.
auth-service remains the global identity/platform-role authority. This explicitly
differs from BRD §8.1; [ADR-F4-001](adr/ADR-F4-001-membership-authority.md) explains why,
the auth-service alternative, and the migration consequences.

Local transaction locks protect building access/state, unit uniqueness, shares,
history and audit/outbox. Preserve F2 approval and platform-only lifecycle actions.
Ownership events use an outbox; transfer documents have separate protected metadata.

## Reviewable artifacts

- [Requirements and acceptance examples](REQUIREMENTS.md)
- [Decisions and alternatives](DECISIONS.md)
- [Feature HLD](ARCHITECTURE.md)
- [Detailed schema/API/security/migration/test proposal](TECH-SPEC.md)
- [Delivery sequence](DELIVERY-PLAN.md)
- [Scoped baseline](BASELINE.md)

## Approval boundary

Approval should explicitly cover the product defaults, supplementary behavior and
the proposed technical design/ADR, or identify changes. Example:
"Approve the F4 proposal and technical design as written."

After that decision, the agent can record the resolved requirements, submit the
technical-readiness evidence, record the approved technical gate against those
artifacts and begin bounded implementation tasks. This packet itself is neither
approval nor a readiness result. Device QA/release approval will still apply later.


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
