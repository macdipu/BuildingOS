# EPIC-BOS-008: Reporting projections, role dashboards, reports and export

Status: PROPOSED (planning draft 2026-10-22; not approved)

Backlog row: [BOS-001/BACKLOG.md](../BOS-001/BACKLOG.md) — "Reporting projections, role dashboards,
PDF/Excel"; depends on "relevant upstream domain events"; BRD 43–47, 86, 111, 116, 132–134.
Covers §146 MVP success criteria 21 (role-specific dashboards) and 22 (export core financial reports).

## Objective

Give each building role (building admin/manager, owner, tenant, committee) a dashboard backed by
denormalized read projections built from Kafka domain events, plus a Reports screen with
PDF/Excel export of the core financial reports, within the §134 performance targets.

## Business Context

- §8.9: the Reporting Service owns aggregated read models, building/owner/manager/committee
  dashboard projections, monthly statements, six-month reports and export-ready views; it
  "should generally consume Kafka events and maintain denormalized read projections" and avoid
  runtime cross-service joins (also §106, §144 anti-pattern "Couple reporting screens to many live
  cross-service joins").
- §9: read models are owned by Reporting in `reporting_db`; database-per-service is mandatory.
- §13: queries such as `GetOwnerDashboardQuery`, `GetFinancialSummaryQuery` use Reporting projections.
- §17, §126, §127, §143 Scenario E: Reporting consumes `lease.activated`, `payment.*`,
  `ownership.transferred` and must update/rebuild dashboard projections.
- §37 + BOS-011 D-02 (done): per-role nav; the Dashboard/Home tab is currently a placeholder
  (`user_app/lib/app/shell/`), mockups `mobile/05..09` (UI-INDEX) are unassigned to any task.
- §110: every projection row is scoped by `building_id`; cross-building views only through explicit
  portfolio/reporting use cases (§149.14 `My Properties`).
- §134: Dashboard P95 < 1 s using read models; mobile list first cached render near-immediate.
- §123 MVP sequence item 26 "Basic reports"; §123 Phase 6 "Kafka projections, Dashboards, PDF, Excel".

## Scope

### Owning service(s)

- **New `reporting-service`** (BRD §8.9, §9, §145 required service set, gateway route
  `/api/v1/reports/** -> reporting-service` §105). No existing service may own these read models:
  building-service/subscription-service/back-office-service each own their own domain DB and the
  architecture forbids cross-service DB reads (docs/ARCHITECTURE.md rule 6). A new deployable needs
  technical approval → **Q-BOS008-01**.
- **building-service (extend)**: emit `unit.created`, `unit.updated`, `ownership.assigned` through the
  existing outbox (today only `ownership.transferred` is published — `OutboxProperties` default topic
  list). BRD §8.2 lists these events.
- **api-gateway (extend)**: route `/api/v1/reports/**` (§105).
- Upstream producers owned by other epics (no BOS-008 code there): rental-service (BOS-003),
  finance-service (BOS-004), work-service (BOS-006), communication-service (BOS-007).

### Data entities (reporting_db; projections, not sources of truth)

- `processed_event` (consumer idempotency, §14.1 rule 3, §16) and per-projection checkpoint.
- `owner_dashboard_view`, `owner_portfolio_view`, `committee_finance_view` — fields exactly as §111.
- Additional projections required by §43–47 cards (field lists to be derived from those cards, no
  extra metrics): `unit_occupancy_view`, `rent_due_view` (per invoice: due date, billed, allocated,
  outstanding, status), `tenant_summary_view`, `lease_expiry_view`, `maintenance_due_view`,
  `expense_month_view`, `work_open_view`, `building_activity_view` (upcoming meeting, latest
  announcement — metadata only; §150.8 forbids copying chat bodies).
- Report row views for §116 (Rent Collection, Owner Statement, Expense, Maintenance).

### Events consumed (Kafka, via producers' transactional outbox §15; envelope §14.1)

| Source (epic) | Events (BRD §) |
|---|---|
| building-service (BOS-010 delivered + extension here) | `unit.created`, `unit.updated`, `ownership.assigned`, `ownership.transferred`, `building.activated`, `building.suspended` (§8.2) |
| rental-service (BOS-003) | `tenant.created`, `lease.created`, `lease.activated`, `lease.terminated`, `unit.vacated`, `rent.invoice.created`, `rent.invoice.overdue`, `tenant.notice.submitted` (§8.3) |
| finance-service (BOS-004) | `payment.recorded`, `payment.allocated`, `payment.reversed`, `receipt.generated`, `maintenance.invoice.created`, `maintenance.invoice.overdue`, `expense.created`, `expense.approved`, `advance.balance.changed` (§8.4) |
| work-service (BOS-006) | `work.created`, `work.assigned`, `work.started`, `work.overdue`, `work.completed` (§8.5) |
| communication-service (BOS-007) | `announcement.published`, `announcement.pinned`, `announcement.expired`, `meeting.created`, `meeting.updated` (§8.6) |

reporting-service **publishes no domain events** in the MVP (none listed in §8.9). Unrecoverable
events go to a DLQ (§96). Topic naming follows whatever convention is fixed in **Q-BOS008-02**.

### APIs (all `GET`, under `/api/v1/reports/**`, §105; pagination §118)

- `GET /api/v1/reports/buildings/{buildingId}/dashboard` — role-shaped dashboard (§43–47).
- `GET /api/v1/reports/portfolio` — cross-building owner portfolio (§111 `owner_portfolio_view`, §149.14).
- `GET /api/v1/reports/buildings/{buildingId}/{reportType}` — §116 report rows with §86 filters.
- `GET .../{reportType}/export?format=pdf|xlsx` — §132–133 export (permission `report.export`, §137).
Exact paths are proposals for the technical stage.

### Flutter screens (`user_app`, new feature `reporting`, feature-first GetX)

| Screen | BRD § | Mockup |
|---|---|---|
| Main dashboard (building admin / default) | §43 | `mobile/05-main-dashboard` |
| Owner dashboard | §44 | `mobile/06-owner-dashboard` |
| Manager dashboard | §45 | `mobile/07-manager-dashboard` |
| Tenant dashboard (Home tab) | §46 | `mobile/08-tenant-dashboard` |
| Committee dashboard | §47 | `mobile/09-committee-dashboard` |
| Reports screen + report view | §86, §116 | none (UI-INDEX: plan from BRD + DESIGN.md) |

UI-only mockup elements are excluded per UI-INDEX rule 2 and gap file `gaps/mobile-core.md`:
3% management-fee payout card (06, also C-4), bKash TrxID verify / WhatsApp actions (07),
governance quorum-voting (09, C-13 future scope), "System Mode" banner (05).

### Back-office impact

None required. The back-office SaaS dashboard (§149.18) was delivered in BOS-010 F6 (F6-T9) from
service APIs; moving it onto reporting projections is out of scope (see Out of Scope).

## Out of Scope

- Producing the upstream domain data/events (BOS-003/004/006/007 own them).
- Audit-service and the §90 Audit Log screen; the §86 "Audit export" card → Q-BOS008-07.
- Asynchronous large exports (§133 "can be asynchronous later").
- Six-month reports (§8.9) and report cards lacking BRD column definitions unless Q-BOS008-05 adds them.
- Back-office §149.18 dashboard migration to projections; platform-wide analytics.
- Mockup-only features listed above; polls/AGM (C-13); payment gateway (C-9).
- Security/load/DLQ release evidence (BOS-009) beyond the per-story checks below.

## Stories

1. [STORY-001](stories/STORY-001.md) — Reporting-service foundation, unit/ownership projection, dashboard frame
2. [STORY-002](stories/STORY-002.md) — Owner dashboard and cross-building portfolio projection (§44, §111, §149.14)
3. [STORY-003](stories/STORY-003.md) — Manager dashboard (§45)
4. [STORY-004](stories/STORY-004.md) — Tenant dashboard (§46)
5. [STORY-005](stories/STORY-005.md) — Committee dashboard and committee finance projection (§47, §111)
6. [STORY-006](stories/STORY-006.md) — Main dashboard common cards and permission-driven actions (§43)
7. [STORY-007](stories/STORY-007.md) — Reports screen and core report views (§86, §116)
8. [STORY-008](stories/STORY-008.md) — PDF and Excel export, owner statement, share (§132–133, §44)
9. [STORY-009](stories/STORY-009.md) — Projection rebuild, DLQ, performance verification (§134, §96, §135)

Open questions: [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md).

## Cross-Story Dependencies

- STORY-001 is the prerequisite for all others.
- STORY-002/003/004/005/006 are mutually independent; each ships when its upstream epic's events exist.
- STORY-007 needs the projections of STORY-002 (owner statement) and STORY-005 (maintenance/expense).
- STORY-008 needs STORY-007 (report rows) and STORY-002 (owner statement).
- STORY-009 can start after STORY-001 and is re-run as each dashboard lands.

External: BOS-003 (rental events), BOS-004 (finance events, receipts), BOS-006 (work events),
BOS-007 (announcement/meeting events), BOS-010 building-service outbox, BOS-009 (release evidence).

## Acceptance Criteria

1. Each of the five role dashboards renders only cards/buttons permitted for the caller's role in the
   active building (§43 "Visibility depends on permission"; §138; Flutter never decides authorization §144).
2. All dashboard and report reads come from reporting_db projections; no runtime cross-service joins (§8.9, §106).
3. Every consumer is idempotent on `eventId`; re-delivery or replay yields identical projections (§14.1, §16).
4. Every projection row carries `building_id`; no caller can read another building's or another
   owner's private rows (§110, §143 Scenario A).
5. Ownership transfer updates owner projections (§127, §143 Scenario E).
6. The four §116 reports can be viewed and exported to PDF and Excel meeting §133.
7. Dashboard P95 < 1 s under normal load and cached first render on mobile (§134).
8. Amounts in BDT, Asia/Dhaka month boundaries, localization keys for all UI strings (§109).

## Risks

- Upstream epics BOS-003/004/006/007 are not yet planned/built; their event payloads are undefined, so
  projection schemas can churn (mitigation: contract tests §98 on `contracts/kafka` schemas per event).
- New deployable (reporting-service) adds ops cost; needs technical approval (Q-BOS008-01).
- Entitlement gating of dashboards/reports under the per-user revenue model (BOS-010 D-22/D-23) is
  undecided (Q-BOS008-03) and changes UX for every dashboard.
- Eventual consistency: a dashboard can lag a just-recorded payment; financial truth stays in
  finance-service (§113); UI must show `updated_at`.

## References

- BRD §8.9, §9, §13, §14–17, §37, §43–47, §86, §96, §105, §106, §109–111, §114, §116, §118,
  §132–135, §137–138, §143 A/E, §144, §145, §146 (21–22), §149.14, §150.8.
- `agentic/data/project-context/ui/stitch/UI-INDEX.md` (mobile 05–09; C-1, C-4, C-13);
  `ui/stitch/gaps/mobile-core.md` (05–09); `ui/stitch/gaps/mobile-ops-community.md` (§86 no mockup).
- `agentic/data/project-context/features/BOS-010/DECISIONS.md` D-22, D-23, D-27;
  `features/BOS-010/units-ownership/APPROVAL.md` + ADR-F4-001 (membership authority in building-service).
- `features/BOS-011` D-02 / Q-01 (per-role nav, role precedence) — `user_app/lib/app/shell/nav_sets.dart`.
- `docs/ARCHITECTURE.md`, `agentic/data/project-context/ARCHITECTURE.md`.
