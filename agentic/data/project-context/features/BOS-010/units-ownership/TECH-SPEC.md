# TECH-SPEC-F4 (LLD): Units and ownership

## Status

APPROVED by operator, 2026-09-24. Refines [ARCHITECTURE.md](ARCHITECTURE.md).
Uses the accepted four defaults and supplementary policies. Explicit implementation
approval is recorded below; the harness gate must be recorded before code changes.
If past/future ownership, paid core setup or auth-owned memberships are selected,
revise the affected sections before a TECHNICAL_READY result.

## Requirement-to-Component Mapping

| Requirements | Proposed components |
|---|---|
| UO-01/05/08 | membership/invitation and portfolio use cases; BuildingAccess port |
| UO-02/03/04 | floor/unit domain, shared UnitInput validator, preview/commit batch use cases |
| UO-06/07/08 | ownership allocator, transfer command, current/history repositories |
| UO-09 | ActivateBuildingService + ReactivateBuildingService valid-unit checks |
| UO-10 | mutation audit + outbox repository/worker and versioned contract |
| UO-11 | approved core-feature access policy; future paid features stay isolated |
| UO-12 | Flutter building_portfolio, building_units, unit_ownership features |
| UO-13 | transfer-document use cases/metadata with existing DocumentStorage port |

## LLD / Component Changes

Use domain models/repositories, one package/use-case interface per action,
infrastructure JDBC adapters and presentation request/response mappers. No repository
access from controllers or framework imports in domain. Keep existing ArchUnit rules.

Proposed use cases: listmybuildings, listmyproperties, getbuildingcontext,
inviteowner, listinvitations, claiminvitation, revokeinvitation, listmembers,
revokemembership; createfloor, updatefloor, createunit, updateunit, listunits, getunit,
previewunitbatch, commitunitbatch; assignownership, transferownership,
getownershiphistory, uploadtransferdocument, downloadtransferdocument, removetransferdocument.
Expose OWNER invitations only initially; general staff/delegation and admin removal
remain a separate permission design. Existing F2 admin creation remains unchanged.

### Mutation transaction and authorization

For every building-scoped write: begin transaction → lock building row FOR UPDATE →
read current membership/platform role and lifecycle state → lock relevant units in
UUID order → check expectedVersion/idempotency → validate → write domain/audit/outbox →
commit. Membership revocation and lifecycle state changes acquire the same building
lock. Do not check membership only before entering the transaction.

This provides a defined ordering: if revocation commits first, the mutation is denied;
if the mutation owns the building lock first, it may finish before revocation commits.
Reads check membership in the same database statement/snapshot as the protected data.
Current owner access also requires a current ownership allocation; historical reads
return only records for that subject while membership remains active.

Cross-building and cross-parent lookups constrain both object ID and building ID.
Use consistent not-found responses for hidden objects, and 403 for a known allowed
building action that the role cannot perform. Platform writes require audit reasons.
Building-unit ownership writes are prohibited while SUSPENDED under the proposal.

### Invitation and identity binding

Create an OWNER invitation to a canonical phone with creator/building/expiry.
Proposed configurable default: 7 days. A duplicate live invitation returns the existing
pending invitation instead of creating extra grants. Revoke/expire never deletes audit.

List/claim pending invitations using a validated signed JWT phone and sub; a body-supplied
phone/userId is not authoritative. CurrentActor currently drops phone, so introduce a
separate VerifiedPhoneIdentity input mapped at presentation for claim use cases.
Invalid/missing phone claims deny claim. A repeated successful claim by the same subject
returns its existing membership; another subject cannot claim it. A new invitation
is required to re-enable a revoked membership.

No SMS delivery in this slice; pending invitations appear after login. Ownership can
be assigned after the intended person claims/links membership; pending allocations
before identity linking are not part of the proposed first implementation.

### Ownership model and immediate-date branch

Shares are exact BigDecimal values at scale <=4, strictly positive, bounded by 100.
Reject extra precision instead of silently rounding. The UI accepts today's Asia/Dhaka
date; the server verifies it using injected Clock/ZoneId. Changes take effect at a
server-captured instant inside the transaction, not client midnight.

Each unit has an ordered ownership revision. Each allocation period records owner,
share, start instant/revision and optional end instant/revision. Two operations at an
identical clock instant still have a strict order through the revision. Closing a
period may fill its end marker once; its original owner/share/start are never overwritten.
A partial transfer closes the source's old period and opens its remaining share plus
the recipient's resulting allocation, closing/reopening an existing recipient allocation
where necessary. Same-source/recipient transfer is rejected.

Example: source 60 + co-owner 40; transfer 20 to recipient → source 40 + co-owner 40 +
recipient 20. Only affected allocations close/open. Assignments check current totals
under the unit lock; zero/unowned balance is valid. Never write lease/tenant records.
No generic ownership PUT/DELETE endpoint. Corrections require a new audited action.

### Idempotency

Allocation, transfer, invitation-claim and batch commit accept an operation ID.
Persist a unique actor/action/operation ID plus canonical request fingerprint and
result reference in the same transaction. Same ID/same request returns the prior result;
same ID/different payload → IDEMPOTENCY_CONFLICT. Check current access before returning
a replayed result; revocation must not expose protected historical responses.

## API Contracts

All paths use existing ApiEnvelope/ApiError and authenticated JWTs. Lists are paginated;
sort keys are allowlisted. Proposed page default 20, cap 100. Gateway explicitly forwards
these paths; no /internal path becomes public.

| Method/path | Input / output |
|---|---|
| GET /api/v1/me/buildings | Authorized building summary, roles, status, ownedUnitCount |
| GET /api/v1/me/properties | Only caller's current owned units in authorized buildings |
| GET /api/v1/me/building-invitations | Intended recipient's pending invitations |
| POST /api/v1/me/building-invitations/{id}/claim | operationId; membership result |
| GET /api/v1/buildings/{b} | Authorized building context, capabilities |
| GET/POST /api/v1/buildings/{b}/invitations | Admin list / phone + OWNER + expiry + reason |
| POST .../invitations/{id}/revoke | reason; audited status |
| GET /api/v1/buildings/{b}/members | Admin-only selectable linked identities/roles |
| POST .../members/{id}/revoke | reason + expectedVersion; OWNER membership only |
| GET/POST /api/v1/buildings/{b}/floors | List / floor label + ordering + kind |
| PUT .../floors/{id} | Validated metadata + expectedVersion |
| GET/POST /api/v1/buildings/{b}/units | Authorized list / unit input |
| GET/PUT .../units/{u} | Authorized detail / unit input + expectedVersion |
| POST /api/v1/buildings/{b}/unit-batches/preview | CSV/XLSX or generated rows; row errors/canonical preview |
| POST /api/v1/buildings/{b}/unit-batches/commit | Reviewed rows + operationId; revalidate then atomic create |
| POST .../units/{u}/ownerships | ownerUserId, share, effectiveDate, notes, reason, expectedVersion, operationId |
| POST .../units/{u}/ownership-transfers | sourceOwnerUserId, recipientUserId, share, effectiveDate, optional reference, reason, expectedVersion, operationId |
| GET .../units/{u}/ownership-history | Admin full history; owner only own periods/transfers |
| POST/GET .../units/{u}/ownership-transfers/{t}/documents | Upload / authorized metadata list |
| GET/DELETE .../ownership-transfers/{t}/documents/{d} | Authorized attachment / audited removal |

The abbreviated nested paths above retain /api/v1/buildings/{b} and /units/{u}.
Actual OpenAPI components and controller request DTOs must use one consistent full path.
The existing platform activate/reactivate routes remain platform-only.

Unit input: number, floorId, type, area, optional bedrooms/defaultMaintenanceRate/notes.
Floor kinds support BASEMENT/GROUND/REGULAR/ROOF/COMMON; labels are display data.
Maintenance rate is nonnegative metadata only, never a generated invoice.
No occupancy/rent/due filters until their owning service is implemented.

## Database Impact

Forward-only migrations after V4 (exact numbering assigned at implementation).

| Table | Key constraints / content |
|---|---|
| existing building_membership | Preserve IDs; add OWNER role and REVOKED status support, revision and updated/revoked audit metadata. Keep unique building/user/role. |
| building_invitation | building FK, canonical phone, role, creator, status, expiry, claimed user/time; one live invite per building/phone/role. |
| building_floor | UUID + building FK, label, kind, display order, revision; unique normalized label per building. |
| building_unit | building/floor FKs, normalized number, display number, type, numeric(12,2) area, optional metadata, version; unique building/normalized_number; area >0. |
| ownership_period | unit FK, owner UUID, numeric(7,4) share >0 <=100, start/end instant and revision. Index current allocations and owner history. |
| ownership_transfer | unit, source/recipient UUIDs, numeric(7,4) share, effective date/instant, revision, actor, reason/reference; immutable business fields. |
| ownership_document | transfer FK, opaque object key, original filename, MIME/size, uploader/time and removal audit; no application-document foreign reuse. |
| building_audit | actor, action, entity, building, reason, before/after permitted fields, timestamp, trace ID; immutable append. |
| building_outbox | event UUID/type/version, aggregate/building UUID, aggregate revision, safe payload, createdAt, attempts/nextAttempt/deliveredAt. |
| building_operation | actor/action/operation ID unique, request fingerprint, result entity/version; transactionally stores accepted result. |

Use composite constraints or validated foreign references so a floor/unit/transfer
cannot belong to an unrelated building. Java validation plus database unique/check
constraints enforce independent invariants. Aggregate shares require the unit lock;
a per-row CHECK alone is insufficient. Schema fields named above are a design, not
an applied migration.

### Bulk validation

Preview parses CSV and XLSX as data; never execute formulas or macros. Limit file bytes,
rows, cells and decompression ratio; reject unsupported formats and formula cells.
Parser dependency/version/security checks occur during implementation before installation.
Proposed operational cap: 500 rows per commit; larger imports split only after explicit
review, with each request's atomic boundary visible to the user.

Preview is advisory. Commit revalidates all submitted canonical rows, building/floor
references and current uniqueness within one transaction. Never silently drop invalid
rows or trust a client-carried "validated" flag. Return field/row codes the UI can localize.

## Security

Preserve JWT issuer/audience/key checks. Never store identity tokens in data rows/logs.
Building-admin role assignment/removal and last-admin changes are outside F4's public
membership endpoints. Existing initial admins retain their authority.

Download requires access to both the unit and the transfer; a current owner cannot
read another person's unrelated transfer document. Admins and the source/recipient
with current membership may access the appropriate transfer records under the proposal.
Use attachment and nosniff, bounded validated content and opaque storage keys.
Document deletion records an audited removal; a historical transfer itself is not deleted.
No automatic grant based on an invitation URL alone.

## Error Handling

400 invalid unit/precision/date/batch; 401 invalid authentication; 403 denied action;
404 hidden/missing building/unit/invitation/document; 409 duplicate number, share exceeded,
insufficient source share, stale version, invalid state, expired/revoked invitation,
no valid unit or idempotency conflict; 413 oversized upload; 415 unsupported type.
Map codes to en/bn; do not display exception/SQL/object-store details.
Dependency/storage failure returns retryable service error without partial ownership.

## Observability

Audit all domain mutations and privileged reads where required. Structured logs include
trace/building/entity/event IDs and error category, not phone numbers, OTPs or documents.
Track pending/oldest outbox age, publish failures, transaction conflicts and rejected
authorization. Worker errors do not silently discard committed events.
Emit ownership.transferred with stable event ID, schema version, unit/building IDs,
source/recipient IDs, share, effective instant/revision and transfer ID; omit contact
details and document contents. Honor the existing event-envelope required fields.

## Dependencies

F2 building lifecycle; auth verified global identity; Postgres; existing S3 storage;
Kafka for ownership events; Flutter core network/localization; approved product choices
and membership ADR. No new service, real SMS, production resource or billing integration.

## Migration / Configuration Impact

Preserve V1–V4 and F2 data. Validate existing memberships before applying additive
schema changes. Add invitation TTL, batch bounds, ownership timezone, and outbox worker
configuration. Use local/test credentials already managed outside version control.
Backend rollout precedes UI. Old feature behavior must pass its existing regression suite.

## Compatibility / Rollback

New OWNER/REVOKED values are incompatible with F2-only enum readers. Stop writes and
use a forward fix after activation, or restore pre-F4 database/object backups with the
matching old artifacts. Do not delete owned objects/volumes to simulate rollback.
Outbox/consumer state must be reconciled before replay after restore. A production
restore plan/drill remains a separate release-hardening responsibility.

## Test Strategy

Domain: unit/floor normalization, enum/numeric constraints, partial/co-owner allocation,
zero/unowned balance, excessive share, today/date boundary and ordered same-day history.

Integration: migrations on fresh and F2 data; duplicate create/rename races; allocation/
transfer concurrency; revoke versus mutation ordering; invitation claim/revoke/expiry/
duplicate identity binding; activation/reactivation versus unit mutation; idempotent
retries and mismatched payload; atomic batch rollback; cross-parent IDs.

Events/storage: outbox rollback atomicity, unavailable Kafka and recovery, stable event
ID on duplicates, no sensitive payload; document metadata rollback, orphan cleanup,
content/size rejection and source/recipient access.

Contracts/mobile: explicit gateway paths and direct-service checks, ApiError schemas,
forged context IDs, en/bn forms, empty/denied/error states, transfer confirmation and
upload/batch preview. Use actual outcomes, no hardcoded success or financial placeholders.

Regression: Maven verify, contract checks, Flutter analyze/test, unchanged architecture
rules, local end-to-end building → invitation claim → unit → allocation → partial
transfer → history → revoke. Device QA and release approval remain explicit.

## Open Decisions

UO-D01..04, supplementary policies in DECISIONS.md, and ADR-F4-001 are approved.
The immediate-date/free-core/single-authority branch is selected. Dated ownership,
paid-core limits or auth-owned memberships would require revised design/approval.

## References

[Requirements](REQUIREMENTS.md), [HLD](ARCHITECTURE.md),
[decision packet](APPROVAL.md), [delivery plan](DELIVERY-PLAN.md).


## Operator decision — 2026-09-24

The operator replied **"approve"** to the explicit request to approve the F4 proposal
and technical design. This accepts APPROVAL.md as presented, including UO-D01..04,
supplementary behavior, the HLD/LLD and ADR-F4-001. It authorizes implementation after
the harness records ready requirements/technical evidence and the technical gate.
Release/device-QA approval is separate. No scope expansion is implied.
