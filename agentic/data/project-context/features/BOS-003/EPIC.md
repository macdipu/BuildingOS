# EPIC-BOS-003: Rental core — tenants, lease lifecycle, rent schedule, recurring rent invoices

Status: PROPOSED (planning draft 2026-10-22; not approved)

No sprint dates, hours or capacity are implied. Sizes are relative (S/M/L/XL). Nothing here
is a decision until the operator approves it; BRD gaps are tracked in
[OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) as `Q-BOS-003-NN`.

## Objective

Let an authorized building operator register tenants, create and activate leases on units,
run the lease to its end (notice, termination, move-out, expiry), and have the system issue
monthly rent invoices on a schedule, so that a tenant and the unit's owner can see the lease
and what is due. Delivers BRD §146 MVP success criteria 11 (register tenants), 12 (create
leases) and 13 (generate monthly rent), and §123 Phase 2 (Tenant, Lease, Rent schedule,
Rent invoice).

## Business Context

- BRD §1/§2 (lines 16–48): the platform must handle tenants that change over time, vacant
  flats, and give tenants clear due information; historical tenancy and rent data are kept.
- BRD §4 roles: `PROPERTY_MANAGER / RENT_MANAGER` operate tenant onboarding, lease creation,
  move-in/move-out; `OWNER` views tenant/lease/rent of owned units; `TENANT` views own lease,
  current rent, dues.
- BRD §138 permission mapping (OWNER: `tenant.read/lease.read/rent.read` owned units;
  TENANT: self/own active lease/own invoices; MANAGER: `tenant.* lease.* rent.*`).
- Delivered upstream (BOS-010/011/012): global user + OTP (auth-service), buildings, units,
  ownership, invitations and memberships with `BuildingRole { BUILDING_ADMIN, OWNER }`
  (building-service, membership authority per ADR-F4-001), per-user subscriptions with the
  `rent_management.enabled` entitlement key (subscription-service, D-22/D-23/D-27), per-role
  Flutter nav including a `tenant` nav set that is unreachable until a TENANT role exists
  (BOS-011 D-02 note).
- Backlog: `features/BOS-001/BACKLOG.md` row BOS-003 (deps BOS-002; Q-03).

## Scope

### Owning service(s)

- **New `rental-service`** with its own `rental_db` — BRD §8.3 (responsibilities/entities/
  events), §9 (Tenants/leases/rent invoice → Rental, rental_db; database-per-service is
  mandatory), §91 (Tenant/Lease/Rent invoices → Rental), §105 (gateway routes
  `/api/v1/tenants/**`, `/api/v1/leases/**`, `/api/v1/rent-invoices/**` → rental-service),
  §145 (rental-service in MVP set). Extending an existing service is not allowed by §9 for
  this data. A new deployable still needs technical approval → **Q-BOS-003-01**.
- **building-service (extend)**: add `TENANT` (and possibly `PROPERTY_MANAGER`/`RENT_MANAGER`)
  building roles and a tenant invitation using the existing invite/claim pattern
  (ADR-F4-001 keeps membership authority in building-service) → Q-BOS-003-04, Q-BOS-003-05.
- **api-gateway (extend)**: the three §105 routes.
- **subscription-service**: no change expected; rental-service reads effective entitlement
  `rent_management.enabled` (§149.16) → whose subscription gates it is Q-BOS-003-06 (D-22).
- **Rent-invoice ownership** (BACKLOG Q-03): §8.3/§9/§91 put RentInvoice in Rental; §17
  example says Finance "creates initial rent obligations". This plan assumes Rental owns it
  and Finance consumes `rent.invoice.created` (§8.4 Consumes) → **Q-BOS-003-02** (blocks
  STORY-005).

### Data entities (rental_db; BRD §8.3 core entities)

- `Tenant` (§56 fields; NID optional, encrypted where appropriate, masked, never logged —
  §94, UI-INDEX C-6).
- `Lease`, `LeaseTenant` (multiple occupants, §20), states `DRAFT, PENDING, ACTIVE,
  NOTICE_GIVEN, EXPIRED, TERMINATED, CANCELLED` (§20), `@Version` optimistic lock (§129).
- `RentSchedule` (rent amount versioned or tied to lease period, §20; recurring charges from
  §59: monthly rent, parking charge, other recurring charges, due day, late-fee policy).
- `RentInvoice` (§21 lines: base rent, parking, service, other recurring, adjustments,
  late fee, discount, previous balance reference; statuses `DRAFT, ISSUED, PARTIALLY_PAID,
  PAID, OVERDUE, VOID`; `due = invoice total − allocated payment`).
- `RentAdjustment` (§8.3, §62 `Add Adjustment`).
- `MoveIn`, `MoveOut`, `Notice` (§8.3).
- Lease agreement document metadata (files are documents, not blobs in rental_db — §20;
  storage behind a port as D-28) → Q-BOS-003-07.
- `rental_outbox` (§15), `audit` rows (§114), idempotency records (§16), local read
  projections needed for authorization (unit/ownership) → Q-BOS-003-08.
- No subscription state stored in rental records (§144, §805).

### Events (Kafka `rental.events` topic, §14.2; all via transactional outbox §15)

Publishes (§8.3): `tenant.created`, `lease.created`, `lease.activated`,
`rent.invoice.created`, `rent.invoice.overdue`, `tenant.notice.submitted`,
`lease.terminated`, `unit.vacated`. Each gets a versioned schema under `contracts/kafka/`
following `ownership-transferred.v1.schema.json` (identifiers only, no NID/contact data).

Consumes (§8.3): `ownership.transferred` (contract exists), `unit.created` (no producer yet
→ Q-BOS-003-08), `payment.allocated` (produced by BOS-004; consumer is out of scope here —
see Out of Scope). Consumers are idempotent (§144).

### APIs (BRD §18, §105; pagination §118; error codes §128)

- `POST/GET /api/v1/tenants`, `GET/PATCH /api/v1/tenants/{id}`
- `POST/GET /api/v1/leases`, `GET/PATCH /api/v1/leases/{id}` (draft only),
  `POST /api/v1/leases/{id}/activate` (Idempotency-Key, §16), `/notice`, `/move-out` (§18),
  plus terminate / cancel / renew (§60 buttons) → shapes proposed in stories.
- `GET /api/v1/rent-invoices`, `GET /api/v1/rent-invoices/{id}`, generate (Idempotency-Key,
  §16 "Generate invoice"), adjustment, void.
- Tenant self-reads (`GetCurrentRentDueQuery`, §13) for §46 data needs.
- Errors include `ACTIVE_LEASE_EXISTS`, `VERSION_CONFLICT`, `DUPLICATE_IDEMPOTENCY_KEY`,
  `ACCESS_DENIED`, `BUILDING_CONTEXT_REQUIRED`, `VALIDATION_ERROR` (§128).

### Flutter screens (`user_app`, new feature-first module(s), GetX)

| Screen | BRD | Mockup | Story |
|---|---|---|---|
| Tenant List | §55 | mobile/17-tenant-list | STORY-001 |
| Add Tenant | §56, §94 | mobile/18-add-tenant | STORY-001 |
| Tenant Detail | §57 | mobile/19-tenant-detail | STORY-001 (Profile), 002–004 (Lease, actions) |
| Lease List | §58 | mobile/20-lease-list | STORY-002 |
| Create Lease | §59, §20 | mobile/21-create-lease | STORY-002/003 |
| Lease Detail | §60 | mobile/22-lease-detail | STORY-003/004 |
| Rent Overview | §61, §21 | mobile/23-rent-overview | STORY-006 |
| Rent Invoice Detail | §62 | mobile/24-rent-invoice-detail | STORY-006/007 |
| Unit Detail Tenant/Lease/Rent tabs | §50 | mobile/12-unit-detail | STORY-008 |
| Tenant Home data + My Lease/Invoices | §46, §138 TENANT | mobile/08-tenant-dashboard (layout only; card UI owned by BOS-008 per UI-INDEX) | STORY-009 |

Binding UI decisions: C-6 (NID optional + masked; no DMP police-verification workflow),
C-4 (no platform fee / payout split on 24), C-7 (receipt format §108 — not issued here),
C-1/BOS-011 D-02 (per-role nav). Mockup-only items (bKash "Verification Pending", escrow sync,
legal covenants/lock-in/escalation, stamp-duty checkpoints, family composition, profession,
utility badges, OCR) are not scope (UI-INDEX rule 2; §125 excludes OCR).

### Back-office impact

None required by the BRD for rental. Possible later: `SUPPORT_VIEW_UNITS`-style support
relay to rental reads (D-10/D-36) — not in this epic.

## Out of Scope

- Payment recording, allocation, receipts, advance ledger, security-deposit/advance-rent
  collection, payment reversal (§22–24, §63–65, §108, §113, §126) → BOS-004. Buttons
  `Collect Rent` / `Record Payment` and the §61 "Advance balance total" card are hidden or
  disabled until BOS-004 (BOS-011 Q-02 placeholder pattern).
- Invoice settlement transitions `ISSUED → PARTIALLY_PAID → PAID` driven by
  `payment.allocated` (§8.3 Consumes, §126 Consumers "Rental") — planned with BOS-004's
  allocation story because BOS-004 defines the event (→ Q-BOS-003-03).
- Rent / lease-expiry reminder push notifications (§115) → BOS-007 (notification-service);
  this epic only publishes events and exposes due dates.
- Owner/Manager/Tenant dashboard projections and PDF/Excel exports (§44–46, §55/§58/§61
  `Export`, §111) → BOS-008. Rent Collection Report (§116) → BOS-008.
- Maintenance invoices (§25) → BOS-004.
- DMP police verification, NID OCR (C-6, §125), in-app gateway payment (C-9, §125).
- Back-office console changes.

## Stories

1. [STORY-001](stories/STORY-001.md) — rental-service foundation + tenant registry (L/XL)
2. [STORY-002](stories/STORY-002.md) — Lease drafts and lease list (L)
3. [STORY-003](stories/STORY-003.md) — Lease activation and rent schedule (L)
4. [STORY-004](stories/STORY-004.md) — Notice, termination, move-out, expiry, renewal (L)
5. [STORY-005](stories/STORY-005.md) — Scheduled monthly rent invoice generation (L)
6. [STORY-006](stories/STORY-006.md) — Rent overview and invoice detail (M)
7. [STORY-007](stories/STORY-007.md) — Invoice adjustments, void and overdue detection (M)
8. [STORY-008](stories/STORY-008.md) — Owner read access and Unit Detail rental tabs (M)
9. [STORY-009](stories/STORY-009.md) — Tenant access: invitation, my lease, my invoices, current due (L)

## Cross-Story Dependencies

```text
STORY-001 ─▶ STORY-002 ─▶ STORY-003 ─▶ STORY-004
                              │
                              ├─▶ STORY-005 ─▶ STORY-006 ─▶ STORY-007
                              ├─▶ STORY-008 (also needs STORY-005 for the Rent tab)
                              └─▶ STORY-009 (needs building-service TENANT role; STORY-005 for dues)
```

STORY-004 and STORY-005 can run in parallel after STORY-003. STORY-008 can ship its
Tenant/Lease tabs before STORY-005 and add the Rent tab after.

## Acceptance Criteria

1. An authorized operator can register a tenant (§56) and the tenant appears in the Tenant
   List with §55 filters (§146 #11).
2. An operator can create a lease for a unit and tenant and activate it; a unit never has two
   active primary leases (§20; `ACTIVE_LEASE_EXISTS` §128) (§146 #12).
3. A scheduled job issues one rent invoice per active lease per billing period, safe under
   multiple instances and retries (§112, §16) (§146 #13).
4. Lease end paths (notice, terminate, move-out, expiry) close tenancy and keep history
   immutable (§20); `unit.vacated` is published on move-out (§8.3).
5. Every lease activation and termination is audited with actor + timestamp (§114); every
   published event goes through the outbox in the same transaction (§15).
6. Owners see tenant/lease/rent only for units they own; tenants see only their own lease
   and invoices; other building members and other buildings get `ACCESS_DENIED`/404 (§138,
   §143 Scenario A).
7. NID is optional, validated when supplied, masked by default in UI, encrypted at rest where
   appropriate, never logged, and readable unmasked only by permitted roles (§94, C-6).
8. Ownership transfer does not change an existing lease (§127 "No active lease ownership
   mutation is automatic", §143 Scenario E).
9. Money amounts carry explicit currency BDT; dates use Asia/Dhaka (§109, §113 #9).
10. UI strings use localization keys; screens follow mockups 17–24 layout except C-6 and
    mockup-only exclusions (UI-INDEX rules 2/4).

## Risks

- R1: §17 vs §8.3/§9/§91 invoice ownership (Q-03) unresolved — wrong choice forces rework
  in BOS-004.
- R2: BRD leaves billing rules open (invoice timing, proration, late fee, overdue threshold,
  invoice number format) — STORY-005/007 cannot start without operator answers.
- R3: A new deployable (rental-service) plus building-service role changes widens the change
  surface; authorization across services (membership/ownership lookups) is the main
  correctness risk.
- R4: Per-user entitlement (D-22) vs a building-operated rental module can lock tenants or
  owners out of their own data if gated naively.
- R5: NID handling (encryption, masking, role-restricted reveal) is a privacy risk if done
  late.

## References

- BRD: §4 roles, §8.3, §8.4, §9, §12–18, §20, §21, §37, §46, §50, §55–62, §91, §94, §105,
  §108, §109, §111–115, §117–119, §123 Phase 2, §124, §125, §127–129, §137–138, §143
  (A, B, C, E, I), §144, §145, §146, §149.16.
- `features/BOS-001/BACKLOG.md`, `features/BOS-001/SRS.md` (Q-03).
- `ui/stitch/UI-INDEX.md` (C-1, C-4, C-6, C-7, C-9), `ui/stitch/gaps/mobile-rental-finance.md`
  (17–24), `ui/stitch/gaps/mobile-core.md` (08, 12).
- `ARCHITECTURE.md`, `docs/ARCHITECTURE.md`.
- `features/BOS-010/DECISIONS.md` (D-22..D-28, D-38), `features/BOS-010/units-ownership/
  DECISIONS.md` (UO-D01..D04), `adr/ADR-F4-001-membership-authority.md`,
  `features/BOS-011/DECISIONS.md` (D-02, Q-01, Q-02).
