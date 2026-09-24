# F4 scoped implementation baseline

Status: AVAILABLE for requirements/design; no implementation readiness asserted.
Observed: 2026-09-24. Feature implementation baseline: F2 commit f1c6dcd;
checkout at intake 380553b (documentation/kit changes after F2).
Discovery: codebase-memory-mcp fast index of the repository, search_graph,
get_code_snippet, search_code and trace_path. Scope limited to building lifecycle,
identity, entitlements, gateway and Flutter entry points.

## Confirmed reuse and gaps

| Area | Evidence | Finding / implication |
|---|---|---|
| Building lifecycle | building-service building/application/activatebuilding/ActivateBuildingService.java; building/application/BuildingChanges.java | Activation checks active admin count only. BuildingChanges requires a platform admin, locks the building inside UnitOfWork, persists the change and transition. Add the unit precondition under the same consistency boundary; preserve platform-only activation. |
| Membership | building/domain/model/BuildingMembership.java; building/domain/repository/BuildingMembershipRepository.java; building/infrastructure/persistence/JdbcBuildingMembershipRepositoryAdapter.java; V4__building.sql | building-service owns persisted initial admin memberships. Unique (building_id,user_id,role); repository has insert, findByBuilding, countActiveAdmins only. User-scoped reads, invitations and revocation are new work. |
| Identity | auth-service auth/application/verifyotp/VerifyOtpService.java; auth/presentation/rest/UserProvisioningController.java | OTP verification finds/creates a global user and issues a token with phone/platform roles. F2 provisioning accepts a relayed platform-admin token; it must not be opened to arbitrary building-admin tokens without an authorization design. |
| Units/ownership | Graph search for Unit/Ownership/Invitation classes/interfaces; graph-enriched code search in backend and user_app/lib | No corresponding business implementations discovered. These aggregates, repositories, controllers and screens are new, extending an existing building lifecycle. |
| Gateway | api-gateway routing/config/GatewayRouteConfig.java | F2 paths cover applications and platform buildings. My Buildings, unit, ownership and invitation paths need explicit new routes; internal provisioning remains unforwarded. |
| Flutter | user_app/lib/app/shell/app_shell.dart; F2 REVIEW.md | Home/application flow exists; no building selector/unit/ownership flow discovered. Reuse core networking, errors, localization and architecture conventions, avoiding cross-feature imports. |
| Revenue | subscription-service catalog/domain/model/Feature.java; F5a REQUIREMENTS.md RV-02/03/07 and TECH-SPEC.md §7 | Catalog includes maintenance/rent/work-orders/report/offline flags and optional max_units/max_users limits. No unit/ownership flag, no consumer enforcement yet. Absent limits mean unlimited; whose usage each per-user limit counts needs a product rule. |
| Auditing/events | F2 REVIEW.md and D-13; BRD §§52,127 | F2 has transactional transition rows; D-13 deferred building-lifecycle Kafka events only. Do not assume that defers the separately required ownership.transferred event. |
| Documents | F2 TECH-SPEC.md/REVIEW.md | S3 storage is reusable behind a port, but F2 metadata/authorization belongs to applications. Transfer documents need their own ownership/access linkage; do not expose application documents to owners. |

Paths in the first column refer to backend service src/main/java/com/buildingos
packages unless a full path or migration filename is given.

## Reconciliation decisions needed

1. BRD §8.1 places BuildingMembership/Invitation in Identity, while shipped F2 stores
   initial memberships in building-service. This is a known architecture discrepancy,
   not permission to create a second authority. Technical design must choose a single
   source of truth and, if moving it, preserve initial admins and activation semantics.
   Continuing building-service ownership requires an explicit ADR/technical approval.
2. Owner identity and active membership are distinct. Assignment cannot silently grant
   membership or bypass a pending invitation. Proposal for review: assign to an already
   linked global identity; separately invite and claim by verified phone before granting
   building access. Pending-owner allocations need an explicit policy if included.
3. The D-30 unit prerequisite is confirmed for new activation. Treatment of buildings
   already ACTIVE with zero units, suspended-building edits, and reactivation validation
   must be explicit; no automatic status downgrade is implied.
4. The new feature's authorization/temporal conventions and revenue mapping are product
   inputs. Draft proposals must remain labeled unapproved.
5. The old BOS-002 README/plan is historical and superseded. Reuse its ownership constraints,
   not its direct building creation, stale login baseline or unfinished run instructions.

## Existing verification and proposed additions

F2's recorded regression evidence: backend 197/0; Flutter 50/0 and clean analysis;
contract checks and isolated full-stack smoke passed, operator device check confirmed.
No tests rerun for this documentation-only baseline.

Future QA must cover unauthorized object substitution, immediately revoked membership,
owner-filtered reads/history, concurrent duplicate units, concurrent share allocation and
transfer, dated-boundary cases, atomic bulk validation, activation racing unit changes,
outbox retry/duplicate delivery and migration of current F2 records. UI verification must
include Bangla, loading/empty/error states, confirmation and explicit permission failures.

## Confidence and freshness

Code claims above were sampled directly through the current graph and source snippets;
V4 schema and the current revenue requirements were read directly. Confidence: high for
listed implementations, scoped discovery only for absence claims. Refresh the affected
slice if any of these files changes before implementation; do not re-audit unrelated modules.
