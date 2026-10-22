# STORY-006: Quality gates - contracts, migrations, permission matrix, audit trace

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Turn §97-99 test expectations into blocking, repeatable gates: OpenAPI breaking-change
validation, Kafka schema versioning, Flyway migration validation, a permission-matrix and
tenant-isolation suite, and an audit-completeness check for §146 #24.

## Scope
- Contracts: CI breaking-change validation of `contracts/openapi/*.yaml` against the merge base
  (§98); Kafka schemas under `contracts/kafka` must be versioned (`*.vN.schema.json`), and a
  changed published schema requires a new version (§98, §14.1 rule 7). Extends
  `scripts/check-contracts.py` (kept Python 3.9-compatible, Q-BOS-009-15).
- Typed Dart client consistency check for user_app endpoints in use (§98 "Maintain typed Dart
  API clients").
- Migrations: per service, Flyway `validate` + migrate-from-empty + migrate-from-previous-
  release snapshot on Testcontainers Postgres (§97 migration tests, §99 "No manual production
  schema changes").
- Permission matrix suite: table-driven API tests per endpoint x role (platform roles §4.1,
  building roles §138, D-07, D-10, D-33, D-36, D-38) asserting allow/deny, including
  "platform roles are not building roles", "building admin cannot grant platform roles",
  "subscription status never grants access" (§149.22), and cross-building access returns
  403/404 (§110, §142 "Strict tenant/building isolation").
- Audit completeness: for every privileged mutation listed in §114 and §149.21 that exists
  today, a test asserts an audit row with actor, action, target, before/after where applicable
  (§8.10, D-37); inventory doc marks unbuilt actions NOT YET APPLICABLE.
- Flutter CI job made blocking once its recorded baseline debt is cleared (Q-BOS-009-14).

## Acceptance Criteria
1. A PR that removes or renames an OpenAPI path/field or changes a response type fails CI
   (§98).
2. A PR that edits an existing versioned Kafka schema in a non-additive way fails CI (§98).
3. Each service's migrations validate and apply from empty and from the previous release
   snapshot in CI (§97, §99).
4. Permission matrix covers every currently exposed endpoint in auth, building, subscription,
   back-office services; an endpoint with no matrix row fails the suite (§93 "Server-side
   authorization for every request"; "Never rely on Flutter UI permissions").
5. Tenant isolation: a member of building A gets no data for building B on every
   building-scoped endpoint (§110, §149.22).
6. Audit completeness test passes for every implemented §114/§149.21 action; the inventory is
   committed under this feature folder (§146 #24).
7. Financial-mutation idempotency tests are listed as pending until BOS-004 (§16, §93); not
   claimed.

## Tasks
Proposed task lanes only:
- QA/BE: OpenAPI diff + Kafka schema version check in CI - M
- DB/QA: migration validation job per service - S
- QA/BE: permission matrix + tenant isolation suite - L
- QA/BE: audit completeness inventory + tests - M
- Mobile: Dart client check; Flutter baseline cleanup assessment - S

Size: L (uncertainty: medium-high; matrix size grows with each domain epic).

## Dependencies
- Q-BOS-009-07 (audit-service vs per-service audit), Q-BOS-009-14 (Flutter gate). None hard.

## Out of Scope
- Golden tests for receipts/reports (§97; owned by BOS-004/BOS-008).
- Pen test by external party (Q-BOS-009-09 asks whether required).

## References
BRD §4.1, §8.10, §14.1, §16, §93, §97, §98, §99, §110, §114, §137, §138, §142, §146,
§149.21, §149.22; BOS-010 DECISIONS D-07, D-10, D-33, D-36, D-37, D-38;
scripts/check-contracts.py; .github/workflows/platform.yml.
