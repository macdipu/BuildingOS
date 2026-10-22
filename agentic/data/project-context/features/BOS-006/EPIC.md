# EPIC-BOS-006: Operations — work orders, contractors, basic assets, expense linkage

Status: PROPOSED (planning draft 2026-10-22; not approved)

No dates, hours or capacity are implied. Sizes are relative (S/M/L/XL). Every rule below cites the
BRD (`agentic/data/project-context/BuildingOS_BRD_Agentic_Development.md`), a decision file, or a UI
gap file; everything the BRD leaves open is in [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) (Q-BOS-006-NN)
with a recommended default that is **not** a decision.

## Objective

Let authorized building staff create, assign, track and complete building work (repairs, planned
maintenance, contracts), keep a contractor register and a basic asset register, and link work to
common expenses recorded in BOS-004 — satisfying BRD §146 success criterion 19 ("Create and track
building work") and BRD §123 Phase 4 (Work orders, Contractors, Assets).

## Business Context

- BRD §3.1 puts Work orders, Contractor management and Assets in scope; §125 excludes "Advanced asset
  maintenance" and "Complex workflow engine" from MVP, so this epic delivers the **basic** versions.
- §4.1 COMMITTEE holds "Work management" and "Contractor management"; BUILDING_ADMIN has "Access all
  building data". §137 defines `work.read`, `work.create`, `work.assign`, `work.complete`.
- §8.5 (Work & Maintenance Service), §9 (Work / contractors → work service, `work_db`), §91 (Work
  order, Contractor → Work), §145 (`work-service` in the MVP service set).
- §26 / §70 / §71: an expense optionally references a work order (`work_order_id`, "Related work");
  §74 has `Add Expense` on the work order; §27 has `actual_cost_reference`.
- Revenue model: per-user subscriptions and free tier per BOS-010 D-22..D-27; §149.16 lists the
  `work_orders.enabled` entitlement.
- BOS-001 BACKLOG: depends on BOS-002 (identity/membership) and BOS-004 (expense linkage). Audit and
  outbox ship with the feature, not later.

## Scope

### Owning service(s)

| Concern | Owner | Basis |
|---|---|---|
| Work orders, contractors, assignments, comments, attachments metadata | **new `work-service`** (`work_db`) | §8.5, §9, §91, §145. Building-service may not own this data (§9 database-per-service). **A new service needs technical approval: Q-BOS-006-01.** §145 allows co-deploying work- and communication-service temporarily without merging domains. |
| Assets | `work-service` (recommended) | §30/§83–84 give no owner; §84 `Create Maintenance Work` and §123 Phase 4 group assets with work. **Q-BOS-006-07.** |
| Expenses (incl. `work_order_id`) | finance-service (BOS-004) | §8.4, §9, §26, §91 |
| Building membership / roles (authorization source) | building-service | ADR-F4-001 (ACCEPTED 2026-09-24): building-service is the sole membership authority |
| Entitlement `work_orders.enabled` | subscription-service | §149.16, D-22/D-23/D-27 |
| Work/asset reminders delivered to users | notification (BOS-007) consumes `work.events` | §115 Work Deadline, §112 |
| Read models / dashboards / calendar | reporting (BOS-008) consumes `work.events` | §43–47, §88, §111 |

### Data entities (work_db)

- `work_order` — §27 fields `title, description, priority, status, planned_start, due_date,
  completed_at, contractor_id, estimated_cost, actual_cost_reference, attachments` + §73 `category`
  + `building_id`, `created_by`, `version` (§129 optimistic locking), explicit currency for
  `estimated_cost` (§113.9, §109 BDT default). Optional `asset_id` (Q-BOS-006-09).
- `work_status_transition` — status timeline for §74 (actor + timestamp, §113.10 pattern / §114).
- `work_assignment` — §8.5 WorkAssignment (contractor assignment history).
- `work_comment`, `work_attachment` — §8.5 WorkComment / WorkAttachment, §74.
- `contractor`, `contractor_contact` — §8.5 Contractor / ContractorContact, §75–76 (name, trade,
  phone, contacts).
- `asset` — §30 fields `asset_code, name, category, purchase_date, warranty_expiry, supplier,
  status, maintenance_interval, last_service_date, next_service_date` + `building_id`.
- `work_expense_link` (projection) — expense id/status/amount per work order, fed by finance events
  (STORY-008, Q-BOS-006-12/13).
- `outbox_event` (§15), `audit_event` (§114; per-service audit + `/internal/audit` per the F6
  back-office aggregation pattern).
- Deferred (§8.5 lists but no screen/rule): `WorkTask`, `Quote` — Q-BOS-006-17.

### Events (Kafka topic `work.events`, §14.2; transactional outbox §15; consumer idempotency per BACKLOG)

Published (§8.5): `work.created`, `work.assigned`, `work.started`, `work.overdue`, `work.completed`,
`contractor.created`. Not in the BRD list (need approval, Q-BOS-006-04/08): `work.blocked`,
`work.cancelled`, asset service-due event. Consumed: `finance.events` `expense.created`,
`expense.approved` (§8.4) plus paid/rejected/void status events that BOS-004 must still define
(Q-BOS-006-13). Envelope/schema under `contracts/kafka` (docs/ARCHITECTURE.md rule 6).

### APIs (gateway-routed, `/api/v1`, §18; pagination §118; error codes §128; 409 on version conflict §129)

Path convention (building-scoped as in shipped building-service vs. BRD §18 `/api/v1/work-orders`):
Q-BOS-006-16. Proposed building-scoped shape:

- Contractors: `POST/GET /buildings/{b}/contractors`, `GET/PATCH /buildings/{b}/contractors/{id}`
- Work orders: `POST/GET /buildings/{b}/work-orders` (filters status/priority/contractor/due date,
  search by title/contractor §117), `GET /buildings/{b}/work-orders/{id}`,
  `POST .../{id}/assign|start|block|unblock|complete|cancel` (one use case per action, §12;
  BRD §18 shows `PATCH /work-orders/{id}/status` — Q-BOS-006-16), `POST .../{id}/comments`,
  `POST .../{id}/attachments` (upload intent/confirm, §131)
- Assets: `POST/GET /buildings/{b}/assets`, `GET/PATCH /buildings/{b}/assets/{id}`,
  `POST .../{id}/documents`, `POST .../{id}/maintenance-work`
- Internal: `GET /internal/audit` (back-office aggregation), `GET /internal/platform/info`
- Cross-service (new, in other services): building-service internal membership lookup
  (Q-BOS-006-02); subscription-service entitlement lookup (Q-BOS-006-03); finance-service expense
  `work_order_id` validation (STORY-008).

### Flutter screens (`user_app`, feature-first GetX; new features `work_orders`, `contractors`, `assets`)

| Screen | BRD | Mockup | Story |
|---|---|---|---|
| Work Order List | §72 | mobile/35-work-order-register | STORY-002 |
| Create Work Order | §73 | none (34 is resident intake — see STORY-009) | STORY-002 |
| Work Order Detail + Assign | §74 | mobile/36-technician-assignment-dispatch | STORY-003 |
| Work Order Detail — progress | §74 | mobile/37-job-progress-execution | STORY-003/004 |
| Work Order Detail — complete | §74 | mobile/38-work-order-completion-signoff | STORY-003 |
| Contractor List / Detail | §75 / §76 | none — plan from BRD + `ui/stitch/design-system/DESIGN.md` | STORY-001 |
| Asset List / Detail | §83 / §84 | none — BRD + DESIGN.md | STORY-006/007 |
| Report Issue (resident) | §27 / §73 (partial) | mobile/34-report-issue | STORY-009 (blocked by Q-BOS-006-11) |
| Add Expense from work order | §70, §74 | mobile/32 (BOS-004 screen) | STORY-008 |

Entry point: `Work` bottom-nav tab for roles that have it (§37; per-role nav C-1 / BOS-011 D-02).
Mockup UI-only elements listed in `ui/stitch/gaps/mobile-ops-community.md` are **not scope**; C-9
(no in-app bKash/Nagad "Pay Now" on mobile 38) is binding.

### Back-office impact

Small: add work-service as an audit source and health target in back-office-service aggregation
(F6 pattern, `back-office-service` config lists auth/building/subscription URLs today). No new
back-office screens. STORY-001 Web lane.

## Out of Scope

- Quotes, WorkTask sub-tasks (§8.5 lists, no rule/screen) — Q-BOS-006-17.
- Kanban and Calendar views of work orders (§72 marks them optional); Calendar screen §88 (BOS-008).
- Delivering push/notification messages (BOS-007 consumes `work.events`); dashboards/reports
  (BOS-008).
- Meeting decisions creating work orders (§29) — BOS-007 calls the STORY-002 API later.
- Advanced asset maintenance, predictive maintenance, vendor marketplace (§125, §3.2).
- Everything UI-only in gap entries 34–38: SLA timers/KPI tiles, technician ratings, in-house staff
  roster split (Q-BOS-006-10), society-common vs chargeable billing classification, SKU parts
  catalog, entry-OTP site access, geo-tagged photo pairing, resident star rating, digital signature,
  "Society Administrative Fee", in-app payment (C-9, §3.2, §125).
- Recording expenses themselves (BOS-004 owns create/approve/pay/void).

## Stories

Ordered so each is independently shippable (each builds on the previous ones' merged code only).

| Story | Title | Size |
|---|---|---|
| [STORY-001](stories/STORY-001.md) | work-service foundation + contractor register | L |
| [STORY-002](stories/STORY-002.md) | Create and list work orders | L |
| [STORY-003](stories/STORY-003.md) | Work order lifecycle: assign, start, block, complete | L |
| [STORY-004](stories/STORY-004.md) | Work order comments and attachments (+ contractor documents) | M |
| [STORY-005](stories/STORY-005.md) | Work deadlines and overdue detection | M |
| [STORY-006](stories/STORY-006.md) | Basic asset register | M |
| [STORY-007](stories/STORY-007.md) | Asset maintenance work and service-due reminders | M |
| [STORY-008](stories/STORY-008.md) | Expense linkage to BOS-004 (work cost, contractor payments) | M |
| [STORY-009](stories/STORY-009.md) | Resident issue reporting (conditional) | M |

## Cross-Story Dependencies

- STORY-001 → all (service, gateway routes, authorization, entitlement, audit, outbox).
- STORY-002 → 003 → (004, 005, 007, 008, 009). STORY-006 → 007. 001 → 006.
- STORY-008 also needs the BOS-004 expense slice (finance-service `work_order_id`, expense events).
- STORY-009 needs Q-BOS-006-11 answered; until then it is not planned into a sprint.
- External: building-service internal membership lookup and building roles beyond
  `BUILDING_ADMIN`/`OWNER` (only these two exist in `BuildingRole` today); subscription-service
  entitlement lookup.

## Acceptance Criteria

1. An authorized building staff member can create, assign, start, block, and complete a work order
   and see its status timeline (§72–74, §8.5 statuses, §146.19).
2. Contractors can be registered and viewed with their current/past works (§75–76).
3. Assets can be registered with §30 fields and linked maintenance work history (§83–84).
4. An expense recorded in BOS-004 with `work_order_id` shows on the work order's cost and on the
   contractor's payments (§26, §70–71, §74–76).
5. Every request is building-scoped and denied across buildings (§5, §110); permissions follow §137.
6. Work completion and other privileged mutations are audited (§114) and visible via back-office
   audit aggregation.
7. `work.*` and `contractor.created` events are written through an outbox in the same transaction
   (§15) and consumers are idempotent.
8. Concurrent edits to a work order return 409 `VERSION_CONFLICT` (§129, §128).
9. Feature is gated by the `work_orders.enabled` entitlement enforced in the backend (§149.16,
   D-22/D-23) — exact subject per Q-BOS-006-03.

## Risks

- New deployable (work-service) adds infra/CI/gateway/compose work and needs technical approval
  (Q-BOS-006-01).
- Authorization depends on building roles (COMMITTEE, etc.) that building-service does not model yet;
  without them only BUILDING_ADMIN can use the feature (Q-BOS-006-02).
- STORY-008 depends on BOS-004 contracts that are not yet planned (expense status events, vendor ↔
  contractor linkage).
- BRD leaves status transitions, asset status and categories open; wrong defaults force rework.
- Per-user entitlement model (D-22) vs. building-level operations: a staff member without a
  subscription could be blocked from building work (Q-BOS-006-03).

## References

- BRD §3, §4.1, §5, §8.5, §8.4, §8.8, §9, §12, §14.2, §15, §16, §18, §26, §27, §29, §30, §37, §70–76,
  §83–84, §88, §91, §109, §112–115, §117–119, §123 Phase 4, §125, §128–131, §137–138, §145–146,
  §149.16.
- `agentic/data/project-context/features/BOS-001/BACKLOG.md`
- `agentic/data/project-context/ui/stitch/UI-INDEX.md` (C-1, C-9), `ui/stitch/gaps/mobile-ops-community.md`
  (34–38), `ui/stitch/design-system/DESIGN.md`
- `agentic/data/project-context/features/BOS-010/DECISIONS.md` D-22..D-28;
  `features/BOS-010/units-ownership/adr/ADR-F4-001-membership-authority.md`
- `agentic/data/project-context/ARCHITECTURE.md`, `docs/ARCHITECTURE.md`
- [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md)
