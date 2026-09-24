# ARCH-F4 (HLD): Units and ownership

## Status

APPROVED by operator, 2026-09-24. Applies the accepted defaults in
[DECISIONS.md](DECISIONS.md). Harness technical-readiness/gate recording follows
final evidence registration; changed scope requires targeted revision.

## Requirements Covered

UO-01..13: authorized building access, units/bulk setup, global-owner linking,
co-ownership/transfers/history, activation, events/documents and Flutter flows.

## Existing Patterns / Components Reused

- auth-service's global user, canonical phone and signed JWT; no new login/provider.
- building-service's Building, membership persistence, JDBC repositories, UnitOfWork,
  locked lifecycle changes and platform admin roles.
- Existing DocumentStorage/S3 boundary, with separate transfer metadata and policies.
- ApiEnvelope/ApiError, gateway pass-through routes and per-service JWT validation.
- Flutter feature-first GetX/domain/data/presentation structure and core networking.

## Component Responsibilities

| Component | Current / proposed responsibility |
|---|---|
| auth-service | Remains global identity and platform-role authority; publishes verified subject/phone claims. F2 internal provisioning remains platform-admin-only. |
| building-service membership feature | Proposed canonical building roles/invitations/revocation, extending F2's existing table; no duplicate auth-service membership table. |
| building-service unit feature | Floor/unit validation, individual and bulk operations, normalized number uniqueness, permitted reads. |
| building-service ownership feature | Allocation periods, share/transfer rules, filtered history, transfer documents and durable events. |
| building-service portfolio use cases | Authorized building and owned-unit reads from its own database; no financial/reporting projection yet. |
| subscription-service | Existing per-user entitlements. Under proposed UO-D04, core setup does not require a plan; paid feature enforcement stays with the corresponding feature. |
| api-gateway | Explicit public route forwarding; never grants building/object access itself. |
| user_app | Building and property navigation plus invitation/unit/ownership forms; UI permissions are advisory. |

Membership placement differs from BRD §8.1; see the proposed
[ADR-F4-001](adr/ADR-F4-001-membership-authority.md). Do not implement until approved.

## Data / Control Flow

1. Authenticated caller enters a building through server-filtered My Buildings.
2. For a mutation, building-service starts a local transaction, locks the building,
   rechecks its state and current membership/permission, then locks affected units
   in UUID order. Revocation/lifecycle mutations use the same building lock.
3. Validate values and any expected version; write domain changes plus audit/outbox
   atomically. Return an envelope after commit.
4. Invitation claims use JWT subject and canonical signed phone, never a body-supplied
   identity. A claim grants only the previously invited role and is idempotent.
5. Ownership assignment requires an existing linked building member under the proposed
   first implementation. The admin can invite an unlinked person first; no pending
   ownership percentage is created before claim in this proposal.
6. The outbox worker publishes committed ownership events and marks delivery after
   broker acknowledgment. Duplicate delivery is possible; event IDs remain stable.

A linked owner's membership and ownership remain separate. A transfer does not revoke
the former owner's membership or grant membership to the recipient.

## Contracts Affected

OpenAPI: My Buildings/Properties, building/floor/unit reads and writes, invitation
claim/revoke, membership list/revoke, allocation/transfer/history/document endpoints.
Kafka: versioned ownership.transferred payload within the existing event envelope.
F2 activation adds NO_VALID_UNIT. No application-approval payload changes are needed.

## Failure Handling

Invalid or unauthorized requests leave no domain changes. Batch commit is atomic.
Membership queries are local and authoritative; database failure denies the operation.
Object storage precedes a document-metadata transaction; on failure remove the staged
object best-effort and expose no row. Cleanup handles orphans.
Kafka failure keeps the committed outbox row pending with observable retry status.
No distributed transaction or business mutation from a client cache.

## Security Boundaries

A JWT identifies a person; platform roles and database membership decide actions.
Every nested unit/ownership/document lookup includes its building parent. Owner
history queries filter by the requesting subject and do not expose unrelated contact
details. Non-members receive a consistent not-found/denied response.
Invite claims cannot reactivate a revoked member without a new invitation.
No platform-role grant or last-admin removal is exposed through the F4 membership API.

## Alternatives Considered

Move membership into auth-service now: matches BRD placement, but changes F2's
atomic building/admin creation and introduces remote consistency/revocation concerns.
Keep a mirror in both services: rejected as a dual-authority failure mode.
Keep one authority in building-service: proposed because it preserves current F2
transactions and permits serializing revocation and building mutations locally.
An eventual move needs an explicit migration/contract, not a silent second copy.

## Migration / Rollback Impact

Add forward-only migrations after V4; preserve existing membership IDs and active
initial admins. Existing active buildings are not demoted. New activation/reactivation
checks apply after rollout. Deploy backend before clients that expose F4.
Once new roles/data exist, do not run old binaries that parse only BUILDING_ADMIN/ACTIVE.
Use a forward fix or restore coordinated pre-F4 database/object snapshots with matching
binaries. No destructive rollback or down migration is part of normal rollout.

## Risks

Coarse building locking favors correctness over maximum write throughput; measure
before optimizing. Membership placement requires an approved BRD deviation.
Today-only policy excludes historical imports of ownership; import units independently.
Personal/contact/document data must remain out of logs and outbox payloads.
Revenue interpretation and business defaults remain unapproved.

## ADR Recommendation

ADR-F4-001 is required. Normal JDBC/unit schema/use-case patterns need no extra ADR.

## References

[Requirements](REQUIREMENTS.md), [baseline](BASELINE.md),
[LLD](TECH-SPEC.md), [architecture standard](../../../../../../docs/ARCHITECTURE.md).


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
