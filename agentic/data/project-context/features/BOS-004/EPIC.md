# EPIC-BOS-004: Finance core — payments, allocation, advance, receipts, reversal, maintenance fees, expenses

Status: PROPOSED (planning draft 2026-10-22; not approved)

Work item: BOS-004 (backlog: `features/BOS-001/BACKLOG.md`). Phase 3 – Finance Core (BRD §123).
Open questions: [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) (`Q-BOS-004-NN`). Nothing in this file is a
decision; every "recommended default" lives in OPEN-QUESTIONS.md and needs explicit operator approval.

## Objective

Give a building an append-only, auditable money trail: generate maintenance fees, record full /
partial / advance payments allocated to invoices (auto oldest-first or manual), hold overpayment as an
advance ledger balance, issue server-numbered receipts (screen + PDF), reverse payments without deleting
them, and run common expenses through the BRD status lifecycle. Satisfies BRD §146 MVP criteria 14–18
and the finance part of 23–24.

## Business Context

- §1/§2: reduce cash-payment disputes with immutable transaction history and receipts (§2 goal 2);
  committees separately manage common building finances (§1).
- §22 allocation model, §23 advance is a ledger balance (not a negative invoice), §24 reversal never
  deletes, §25 maintenance fee rules, §26 expense rules, §113 financial invariants 1–10, §114 audit,
  §16 idempotency for financial mutations, §108 server receipt numbering, §132 PDF receipt contents.
- Acceptance scenarios §143 B (partial rent) and C (advance rent).
- Revenue model: per-user subscriptions and free tier = maintenance (BOS-010 D-22, D-23, D-27). The BRD
  building-level subscription (§110.3) is superseded and must not be embedded in finance entities (§8.12).
- Binding UI decisions (UI-INDEX, operator 2026-09-25): C-4 no platform fee / payout split; C-5 §63
  allocation UI (invoice pick, auto/manual, summary); C-7 §108 server receipt format; C-8 §26 expense
  status enum; C-9 no in-app gateway payment.

## Scope

### Owning service(s)

- **finance-service (new, `finance_db`)** — BRD §8.4, §9 (Payments / expenses / maintenance → Finance,
  `finance_db`), §91 (Payments, Maintenance invoices, Expense → Finance), §145 (finance-service in the MVP
  set). Extending building-service would violate §9 database-per-service ownership and
  docs/ARCHITECTURE.md rule 6; subscription-service (D-18) is the precedent for adding a BRD service.
  **New service needs technical approval → Q-BOS-004-01.**
- **building-service (existing, read-only dependency)** — authoritative for building context,
  memberships/roles, units (`area_sqft`, `default_maintenance_rate`) and ownership periods. Finance reads
  them over HTTP (caller token relay, fail-closed; ARCHITECTURE.md) — Q-BOS-004-31. A role-catalogue
  extension (COMMITTEE, PROPERTY_MANAGER, ACCOUNTANT, VIEWER) belongs to building-service, not this epic
  (Q-BOS-004-02).
- **subscription-service (existing)** — entitlement check only for paid finance features (Q-BOS-004-21).
- **rental-service (BOS-003, not yet built)** — owns rent invoices (§9); STORY-008 only.
- **api-gateway** — explicit finance routes (`/api/v1/payments`, `/api/v1/maintenance-invoices`,
  `/api/v1/expenses`, …) per §18.

### Data entities (finance_db; §8.4 core entities)

`MaintenanceInvoice` (§25 fields), `MaintenanceGenerationRun` (billing month, rate policy, scope,
exclusions — supports §67 duplicate prevention), `Payment` (§24 reversal fields, §92 fields, `@Version`
§129), `PaymentAllocation`, `AdvanceLedger` entries (§23), `Receipt` + receipt sequence (§108),
`Expense` (§26 field list) + `ExpenseStatusTransition`, `ExpenseCategory`, finance building settings
(receipt prefix, expense categories; §89 Finance), `RentInvoiceProjection` (STORY-008), `IdempotencyKey`
record (§16), `finance_audit` (§114), `finance_outbox` (§15).
Not in scope: `FinancialPeriod`, `ReconciliationRecord`, `PaymentMethod` as configurable entity — the §8.4
payment-method enum is used as-is (Q-BOS-004-15, out of scope list).

### Events (Kafka topic `finance.events`, §14.2; transactional outbox §15; versioned envelope §14.1)

Publishes (§8.4): `maintenance.invoice.created`, `maintenance.invoice.overdue`, `payment.recorded`,
`payment.allocated`, `payment.reversed`, `receipt.generated`, `advance.balance.changed`,
`expense.created`, `expense.approved`. Consumes (STORY-008, §8.4): `rent.invoice.created`
(+ any rent-invoice adjust/void events BOS-003 defines — Q-BOS-004-22). `lease.activated` /
`unit.created` consumption is not needed by any story here (units are read on demand). Each event gets a
JSON schema in `contracts/kafka/` (existing `event-envelope.schema.json` pattern). Consumers are
idempotent by `eventId` (§14.1 rule 3).

### APIs (§18, §92; all mutations require `Idempotency-Key` §16 and building context §5)

- `POST /api/v1/maintenance-invoices/preview`, `POST /api/v1/maintenance-invoices/generate`,
  `GET /api/v1/maintenance-invoices` (filters: status, month, unit), `GET /api/v1/maintenance-invoices/{id}`,
  `GET /api/v1/maintenance-invoices/summary` (§66 cards).
- `POST /api/v1/payments` (§92 shape), `GET /api/v1/payments`, `GET /api/v1/payments/{id}`,
  `GET /api/v1/payments/{id}/receipt.pdf`, `POST /api/v1/payments/{id}/reverse` (§18).
- `POST /api/v1/advance-ledgers/{id}/allocations` (explicit advance consumption, §23), `GET` balances.
- `POST /api/v1/expenses`, `PATCH /api/v1/expenses/{id}`, `POST /api/v1/expenses/{id}/{submit|approve|reject|mark-paid|void}`,
  `GET /api/v1/expenses`, `GET /api/v1/expenses/{id}`; expense categories CRUD; finance settings.
- Error codes per §128 (`PAYMENT_ALLOCATION_EXCEEDS_AMOUNT`, `PAYMENT_ALREADY_REVERSED`,
  `INVOICE_ALREADY_PAID`, `DUPLICATE_IDEMPOTENCY_KEY`, `VERSION_CONFLICT`, …); HTTP per §130 (409 on
  version conflict §129). OpenAPI in `contracts/openapi/`.

### Flutter screens (`user_app`, new feature `features/finance/`, GetX feature-first; docs/ARCHITECTURE.md §Flutter)

| Screen | BRD | Mockup | Story |
|---|---|---|---|
| Generate Maintenance Fees | §67 | mobile/29 | STORY-001 |
| Maintenance Payment | §68 (+§63 allocation UI per C-5) | mobile/30 | STORY-002 |
| Payment Detail (+ receipt PDF) | §64, §108, §132 | mobile/26 | STORY-003 |
| Payment Reversal | §65 | mobile/27 | STORY-004 |
| Maintenance Fee Dashboard | §66 | mobile/28 | STORY-006 |
| Expense List / Add / Detail | §69 / §70 / §71 | mobile/31 / 32 / 33 | STORY-007 |
| Collect Rent | §63, §126 | mobile/25 | STORY-008 |

Entry points: existing `ShellTab.finance` (manager/committee) and `ShellTab.payments` (owner/tenant) in
`user_app/lib/app/shell/nav_sets.dart` (C-1 per-role nav). Mockups govern layout only; UI-only elements
flagged in `ui/stitch/gaps/mobile-rental-finance.md` are excluded (see Out of Scope).

### Back-office impact

None required for this epic. Follow-ups (not in scope): D-36 marks `SUPPORT_VIEW_PAYMENTS` (beyond
creation-fee payments) and `SUPPORT_REVERSE_PAYMENT` as "grantable, not actionable, until their owning
features exist" — once STORY-003/004 ship, a back-office relay task (F6-T4b style) can make them
actionable. Raise as a separate CR.

## Out of Scope

- Platform fee / landlord-fund payout split (C-4; gap file flag 1).
- Payment gateway, bKash/Nagad merchant APIs, QR payment, in-app "Pay Now" (§3.2, §125, C-9), including
  D-24 "payment-gateway collection later" of the creation fee (Q-BOS-004-26).
- Treasury / bank-account "Deposited To", reserve / sinking funds, budget burn cards (mockups 28/30/31;
  no BRD rule; §125 "Full accounting double-entry ledger" deferred) (Q-BOS-004-27).
- Partial reversal and dual-signoff reversal / named multi-stage expense sign-off (mockups 27/32/33;
  §125 "Complex workflow engine" deferred).
- QR / hash verification on receipts (mockup 26, UI-only).
- Reports and exports (`Export Report` §66, `Export` §69) → BOS-008 (Q-BOS-004-23).
- Receipt push/SMS notifications → BOS-007 (§126 Notification consumer). "Share" uses the OS share sheet.
- Rent invoice generation, tenants, leases → BOS-003. Work orders → BOS-006 (expense link only).
- Reconciliation, financial periods, owner income summaries (§8.4 responsibilities not covered by §146
  criteria 14–18) — later CR.
- Role-catalogue extension in building-service (dependency, Q-BOS-004-02).

## Stories

| Story | Title | Size |
|---|---|---|
| [STORY-001](stories/STORY-001.md) | Finance service foundation + maintenance fee generation (preview/generate) | XL |
| [STORY-002](stories/STORY-002.md) | Record maintenance payment with allocation, advance credit and server receipt number | L |
| [STORY-003](stories/STORY-003.md) | Payment detail, payment history and PDF receipt | L |
| [STORY-004](stories/STORY-004.md) | Payment reversal | M |
| [STORY-005](stories/STORY-005.md) | Consume advance balance through explicit allocation | M |
| [STORY-006](stories/STORY-006.md) | Maintenance fee dashboard, owner dues view, overdue detection | M |
| [STORY-007](stories/STORY-007.md) | Expense lifecycle (register, add, detail, status enum, receipt document) | L |
| [STORY-008](stories/STORY-008.md) | Collect rent against rent invoices (Scenarios B/C) | L |

## Cross-Story Dependencies

```text
STORY-001 ─┬─> STORY-002 ─┬─> STORY-003
           │              ├─> STORY-004 (needs 003 for detail entry point; can ship API-first)
           │              ├─> STORY-005
           │              └─> STORY-006 (collected/outstanding need allocations)
           ├─> STORY-007 (needs only the 001 foundation)
           └─> STORY-008 (needs 002 + 003; external: BOS-003 rent invoices)
```

STORY-007 can run in parallel with STORY-002..006. STORY-008 is the only story blocked by BOS-003.

## Acceptance Criteria (epic level)

1. §146 criteria 14–18 demonstrable end-to-end in local compose: full/partial/advance collection,
   receipt issued, maintenance fees generated and collected, common expense recorded, outstanding
   tracked; §143 Scenarios B and C pass as automated tests (B/C on rent in STORY-008; the same math on
   maintenance in STORY-002).
2. All ten §113 invariants are enforced server-side and covered by tests.
3. Every financial mutation is idempotent (§16), carries actor + timestamp (§113.10), writes an audit row
   in the same transaction (§114: payment creation, payment reversal, expense approval, maintenance fee
   generation), and emits its event through the outbox (§15).
4. Every request is authorized server-side against the active building context; a user of building A can
   never read or mutate building B finance data (§5, §93, tenant isolation).
5. No finalized payment can be deleted through any API (§24, §113.7); due amounts are always derived from
   allocations (§21).
6. UI follows C-4, C-5, C-7, C-8, C-9.

## Risks

1. **New service + missing roles**: finance-service is a new deployable and the backend has only
   `BUILDING_ADMIN`/`OWNER` building roles; §4 finance roles (COMMITTEE, PROPERTY_MANAGER, ACCOUNTANT) do
   not exist yet (Q-BOS-004-01/02/03).
2. **BOS-003 coupling**: rent collection depends on an unplanned rental-service and an unagreed
   settlement-authority contract (Q-BOS-004-22); mitigated by building maintenance-first.
3. **Underspecified finance rules**: receipt code/sequence, maintenance status enum, rate policies,
   adjustment mode, reversal-after-advance-consumption and expense transitions are open
   (Q-BOS-004-04..20); wrong defaults are expensive to undo in append-only data.

## References

- BRD `agentic/data/project-context/BuildingOS_BRD_Agentic_Development.md`: §4–5, §8.4, §9, §13–18, §21–26,
  §63–71, §89, §91–93, §108–109, §112–114, §125–126, §128–132, §143 B/C, §145–146, §149.16.
- `features/BOS-001/BACKLOG.md`; `ui/stitch/UI-INDEX.md` (C-4, C-5, C-7, C-8, C-9);
  `ui/stitch/gaps/mobile-rental-finance.md` (screens 25–33).
- `features/BOS-010/DECISIONS.md` D-18, D-22..D-28, D-36; `ARCHITECTURE.md`; `docs/ARCHITECTURE.md`.
- Existing patterns: building-service `building_outbox` (V8), `audit/`, `BuildingContext.readOnly()`.
