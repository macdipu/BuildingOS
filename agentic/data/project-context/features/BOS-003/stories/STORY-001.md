# STORY-001: rental-service foundation and tenant registry

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
An authorized building operator can register, edit, list and view tenants of the active
building from the app, backed by a new rental-service that owns `rental_db`. This is the
first usable rental slice and the scaffold every later story builds on.

## Scope
- New Maven module `backend/rental-service` (Clean Architecture, feature-first, one use case
  per action, JDBC + Flyway, ArchUnit test — docs/ARCHITECTURE.md rules 1–8), own
  `rental_db` and credentials (BRD §9), local compose entry, readiness endpoint, correlation
  and structured logs (§95), `rental_outbox` + publisher job copied from the building-service
  outbox pattern (§15), audit table + internal audit read endpoint (pattern of
  building-service `/internal/audit`, consumed by back-office aggregation later).
- api-gateway route `/api/v1/tenants/**` → rental-service (§105).
- Building-context authorization: caller must be a member of the building with a write role
  (Q-BOS-003-04); building status gate (Q-BOS-003-11); resolution of caller membership via
  building-service (Q-BOS-003-08).
- `tenant` feature: `CreateTenantUseCase` (§12), update, get, list (search by name/phone
  §117, pagination §118, filters §55).
- Tenant fields per §56: full name, phone, email (optional), NID (optional), emergency
  contact, permanent address, occupants count, photo (optional), notes.
- Event `tenant.created` v1 via outbox; contract `contracts/kafka/tenant-created.v1.schema.json`
  (identifiers only, no NID/phone).
- Flutter feature (e.g. `features/rental`): Tenant List (mobile/17), Add Tenant (mobile/18),
  Tenant Detail Profile tab (mobile/19); entry point from the manager nav (BOS-011 D-02).

## Acceptance Criteria
1. `POST /api/v1/tenants` with a valid body creates a tenant in the caller's active building
   and returns it; name is required, phone must match the platform phone format (`+880`
   support, §109), email/NID/photo/notes optional (§56 Validation).
2. A supplied NID is validated for format (§56 "validate if supplied"); an invalid NID is
   rejected with `VALIDATION_ERROR` (§128). An omitted NID is accepted (§94, C-6).
3. NID is never written to logs, events or audit diffs in clear (§94); it is stored encrypted
   where appropriate (§94) and every API response returns it masked unless the caller holds
   the reveal permission (Q-BOS-003-09); a reveal is audited (§94 "Audit access … where
   practical").
4. The Add Tenant screen shows NID as optional and masked by default; no DMP police-
   verification panel, no NID OCR, no family-composition/profession fields (C-6, §125,
   UI-INDEX rule 2).
5. `Save Tenant` saves only the tenant; `Save & Create Lease` saves and opens Create Lease
   pre-filled (§56 Buttons) — the Create Lease target lands with STORY-002; until then the
   button is hidden.
6. Tenant List shows Tenant, Flat, Rent, Due, Status (§55 Data) with filters Active / Notice /
   Moved out / Unit / Rent overdue (§55); filters and columns whose data does not exist yet
   (Flat/Rent before STORY-002/003, Due/Rent overdue before STORY-005) render empty/disabled,
   not fabricated.
7. Search by tenant name and phone works and lists are paginated with the §118 envelope.
8. A caller without a write role in the building gets `ACCESS_DENIED`; a caller with no
   membership in that building gets 404/`ACCESS_DENIED` without leaking existence; a tenant
   of building A is never returned for building B (tenant isolation, §5/§110, §144).
9. Each create/update writes an audit row (actor, timestamp, before/after without NID) in the
   same transaction (§113 #10 spirit, §114 building configuration/role-change pattern), and
   `tenant.created` is inserted into the outbox in the same transaction as the tenant (§15);
   the publisher retries until Kafka acknowledges.
10. A repeated create with the same `Idempotency-Key` returns the original tenant and creates
    no duplicate (§16 pattern; tenant creation is not in §16's critical list, so this is a
    recommended safeguard → Q-BOS-003-10).
11. Concurrent edits to one tenant are rejected with `VERSION_CONFLICT` (§128, §129 pattern).
12. The rental entitlement check (Q-BOS-003-06) is enforced on the backend; the app only hides
    UI (§149.16 rules).
13. UI strings use localization keys (§109); layout follows mobile/17–19 within these rules.

## Tasks
Proposed task lanes (no TASK files yet):
- BE/DB: scaffold rental-service module, Flyway V1 (tenant, audit, outbox, idempotency),
  JWT validation, ArchUnit — L
- BE: tenant use cases (create/update/get/list/search), NID encryption + masking + reveal
  audit, membership/role lookup client — M
- BE: outbox publisher + `tenant.created` contract + `scripts/check-contracts.py` coverage — S
- Infra: compose service, gateway route, Postgres init for rental_db — S
- Mobile: rental feature scaffold (feature_generator), Tenant List / Add Tenant / Tenant
  Detail (Profile tab), masked NID widget, l10n keys — M
- QA: authorization matrix tests (admin / owner / unrelated member / other building), NID
  log-scrub test, outbox atomicity test, Flutter widget tests — M

Size: **L/XL** (new deployable; uncertainty high until Q-BOS-003-01/04/08 are answered).

## Dependencies
- BOS-010 delivered: auth-service JWT/JWKS, building-service memberships/buildings, gateway,
  subscription-service entitlements.
- Questions: Q-BOS-003-01, -04, -05, -06, -08, -09, -10, -11.

## Out of Scope
- Tenant invitations / TENANT app access (STORY-009).
- Lease, rent, payment tabs content (STORY-002..007, BOS-004).
- `Export` on Tenant List (§55) → BOS-008 (Q-BOS-003-28).
- Tenant List "Verification Pending (bKash)", utility badges, occupancy card (UI-only).

## References
BRD §5, §8.3, §9, §12, §15, §16, §55, §56, §57, §94, §95, §105, §109, §110, §117, §118,
§128, §129, §144, §149.16; UI-INDEX C-6; gaps/mobile-rental-finance.md 17, 18, 19;
docs/ARCHITECTURE.md; BOS-011 D-02.
