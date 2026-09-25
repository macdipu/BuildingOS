# Stitch UI index — screen ↔ BRD ↔ work item ↔ task

Status: DRAFT (imported 2026-09-25 from a Google Stitch export; not human-approved as scope).
BRD: [`../../BuildingOS_BRD_Agentic_Development.md`](../../BuildingOS_BRD_Agentic_Development.md).
Backlog map: [`../../features/BOS-001/BACKLOG.md`](../../features/BOS-001/BACKLOG.md).

## How agents use this

1. **Planning a task that touches a screen** — find the screen below, open its `screen.png`
   and `code.html`, read the linked BRD section, then read the screen's entry in the gap file
   (`gaps/*.md`). Fill the task's `## UI Reference` with the folder + BRD section.
2. **BRD governs behavior, mockup governs layout/style.** BRD-only items (required by BRD,
   missing in mockup) stay in scope and go into acceptance criteria. UI-only items (in mockup,
   not in BRD) are **not scope** — list them under the task's open questions / out of scope and
   raise a CR if wanted (AGENTS.md core rule 9).
3. **Implementing** — map `design-system/DESIGN.md` tokens (colors, Plus Jakarta Sans / Inter
   type scale, radii, spacing) onto the existing theme and shared components. Mockup HTML is
   Tailwind reference, never pasted code. Preview/QA compares against `screen.png`.
4. Where mockup and BRD conflict on a rule (below: **Conflicts**), BRD wins until the operator
   decides otherwise.

Layout: `mobile/<NN-slug>/` → Flutter `user_app` (Mobile persona);
`back-office/<NN-slug>/` → Next.js back-office web (FE persona, BOS-010 F6-T6..T9).
Per-screen detail (Shows / Aligned / UI-only / BRD-only / Open questions):
[back-office](gaps/back-office.md) · [mobile core 01–16](gaps/mobile-core.md) ·
[rental & finance 17–33](gaps/mobile-rental-finance.md) ·
[ops & community 34–43](gaps/mobile-ops-community.md).

## Back-office web (BOS-010 back-office-console)

| Screen | BRD | Req | Task(s) |
|---|---|---|---|
| `back-office/01-multi-tenant-platform-overview-metrics` | §149.18, §149.2 | BOC-09, BOC-01/02 | F6-T9 (nav: F6-T6) |
| `back-office/02-building-onboarding-pipeline` | §149.3–149.7, §149.9–149.11 | BOC-03, BOC-06 | F6-T7, F6-T8 (API F6-T3) |
| `back-office/03-society-subscriptions-billing` | §149.15–149.16, §149.23, §8.12 | BOC-04 | F6-T7 |
| `back-office/04-system-audit-security-log` | §90, §114, §149.21 | BOC-08 | F6-T9 (API F6-T5) |

No mockup: §149.2 **Users** (BOC-05, F6-T8), **Support** — assisted onboarding list, active
support sessions, history (BOC-06/07, F6-T8), System settings / entitlement config / health.

## Mobile app (Flutter `user_app`)

Already-delivered screens (01–04, 10–12, 14) are being aligned by CR
[BOS-011](../../features/BOS-011/CR.md) (tasks UI-T01..UI-T10); completed BOS-010 tasks stay closed.

| Screen | BRD | Work item | Task / existing impl |
|---|---|---|---|
| `mobile/01-splash-screen` | §39 | BOS-010 | none — no splash route (`app_pages.dart` starts at `/login`) |
| `mobile/02-login-screen` | §40 | BOS-010 | TASK-002, TASK-005 — `authentication/.../login_screen.dart` |
| `mobile/03-otp-verification-screen` | §41 | BOS-010 | TASK-002/006/007 — `login_otp_verify_screen.dart` |
| `mobile/04-my-buildings-selector` | §42 | BOS-010 F4 | F4-T7 — `units_ownership/.../property_home_page.dart` |
| `mobile/05-main-dashboard` | §43 | BOS-008 | none (AppShell placeholder) |
| `mobile/06-owner-dashboard` | §44 | BOS-008 | none |
| `mobile/07-manager-dashboard` | §45 | BOS-008 | none |
| `mobile/08-tenant-dashboard` | §46 | BOS-008 | none |
| `mobile/09-committee-dashboard` | §47 | BOS-008 | none |
| `mobile/10-unit-list` | §48 | BOS-010 F4 | F4-T2/T3 — `building_units_page.dart`, `batch_units_page.dart` |
| `mobile/11-add-edit-unit` | §49 | BOS-010 F4 | F4-T2 — `unit_form_page.dart` |
| `mobile/12-unit-detail` | §50 | BOS-010 F4 (+BOS-003/004 tabs) | F4-T2/T4/T7/T8 — `unit_detail_page.dart` |
| — (no mockup) | §51 Ownership Assignment | BOS-010 F4 | `ownership_form_page.dart` |
| `mobile/14-ownership-transfer` | §52, §127, §19.1 | BOS-010 F4 | F4-T4a/T5b/T8 — `ownership_form_page.dart`, `transfer_files_page.dart` |
| `mobile/15-owner-list` | §53 | BOS-010 F4 / BOS-003-004 | none (closest: `members_page.dart`) |
| `mobile/16-owner-detail` | §54 | BOS-010 F4 / BOS-003-004 | none |
| `mobile/17-tenant-list` | §55 | BOS-003 | none yet |
| `mobile/18-add-tenant` | §56, §94 | BOS-003 | none yet |
| `mobile/19-tenant-detail` | §57 | BOS-003 | none yet |
| `mobile/20-lease-list` | §58 | BOS-003 | none yet |
| `mobile/21-create-lease` | §59, §20 | BOS-003 | none yet |
| `mobile/22-lease-detail` | §60 | BOS-003 | none yet |
| `mobile/23-rent-overview` | §61, §21 | BOS-003 | none yet |
| `mobile/24-rent-invoice-detail` | §62 | BOS-003 | none yet |
| `mobile/25-collect-rent` | §63, §22, §126 | BOS-004 | none yet |
| `mobile/26-payment-detail` | §64, §108 | BOS-004 | none yet |
| `mobile/27-payment-reversal` | §65, §24 | BOS-004 | none yet |
| `mobile/28-maintenance-overview` | §66, §25 | BOS-004 | none yet |
| `mobile/29-generate-maintenance-fees` | §67 | BOS-004 | none yet |
| `mobile/30-maintenance-payment` | §68 | BOS-004 | none yet |
| `mobile/31-expense-register` | §69, §26 | BOS-004 | none yet |
| `mobile/32-add-expense-voucher` | §70 | BOS-004 | none yet |
| `mobile/33-expense-detail` | §71 | BOS-004 | none yet |
| `mobile/34-report-issue` | §27, §73 | BOS-006 | none yet |
| `mobile/35-work-order-register` | §72 | BOS-006 | none yet |
| `mobile/36-technician-assignment-dispatch` | §74 (Assign) | BOS-006 | none yet |
| `mobile/37-job-progress-execution` | §74 | BOS-006 | none yet |
| `mobile/38-work-order-completion-signoff` | §74 (Complete) | BOS-006 | none yet |
| `mobile/39-notice-board-circulars` | §77, §150.2 | BOS-007 | none yet |
| `mobile/40-notice-detail-acknowledgment` | §79, §150.2 | BOS-007 | none yet |
| `mobile/41-society-polls-agm-voting` | §3.2 future scope (C-13) | none | — |
| `mobile/42-resident-directory-intercom` | §87 Contacts (partial) | none — §87 in no backlog item | — |
| `mobile/43-visitor-gate-pass-management` | §3.2 future / §125 MVP exclusion (C-13) | none | — |

No mockup (BRD screens): §51 Ownership Assignment, §75–76 Contractors, §78 Create
Announcement, §80–82 Meetings, §83–84 Assets, §85 Notifications, §86 Reports, §88 Calendar,
§89 Settings, §90 Audit Log (mobile), §150.3 Community Chat. Plan these from BRD text + DESIGN.md.

## Conflicts — decisions (operator, 2026-09-25)

"BRD (default)" = BRD wins per AGENTS.md UI rule 2; mockup element is not scope.

| # | Screens | Mockup shows | BRD says | Decision |
|---|---|---|---|---|
| C-1 | mobile 04+ (all) | One bottom nav: Home/Units/Finance/Work/Community | §37: per-role nav sets (Owner, Tenant, Committee, admin differ) | BRD — per-role nav (BOS-011 D-02, done) |
| C-2 | back-office 01, 03 | Automated SaaS billing: bKash/Nagad auto-pay, direct debit, VAT invoices, MRR/GMV, dunning | §149.23: MVP has no automated billing/collection/invoicing/VAT/dunning | BRD (default) — no automated billing in MVP |
| C-3 | back-office 02 | 4-stage kanban, RAJUK deed KYC, per-building DB/subdomain | §149.4 state machine (DRAFT→SUBMITTED→UNDER_REVIEW→…→ACTIVE); §149.5 review fields/actions | BRD (default) — §149.4 states + §149.5 fields |
| C-4 | mobile 24, 25, 26 | Auto 3% platform fee + landlord/fund payout split at collection | §22/§23/§113/§126: no such model | Dropped — no platform fee / payout split |
| C-5 | mobile 25 | Single lump balance, no invoice pick / allocation toggle / summary | §63: selected invoices, auto-oldest/manual allocation, received/allocated/advance/remaining summary | BRD (default) — §63 allocation UI |
| C-6 | mobile 18, 19 | NID required + shown unmasked; DMP police verification workflow | §56/§94: NID optional, masked by default; no DMP workflow | BRD — NID optional + masked; no DMP workflow |
| C-7 | mobile 19, 26 | Receipt `RCP-2025-0302` | §108: server-generated `BLD01-RNT-202609-000123` | BRD (default) — server §108 receipt format |
| C-8 | mobile 31, 33 | Expense statuses "Settled", "Approved & Paid" | §26: DRAFT/SUBMITTED/APPROVED/REJECTED/PAID/VOID | BRD (default) — §26 status enum |
| C-9 | mobile 38 | In-app "Pay Now via bKash/Nagad" | §3.2/§125: payment-gateway integration excluded | BRD (default) — no in-app gateway payment |
| C-10 | mobile 11, 14 | Unit wizard bundles ownership/parking/utility meters; transfer adds OTP gate + legal-instrument type | §49 field list; §52/§127 transfer fields | BRD — mockup extras excluded (BOS-011 D-03, done) |
| C-11 | mobile 02 | "Continue with Google" primary | BOS-010: Google sign-in deferred | BRD — Google sign-in deferred (done) |
| C-12 | mobile 39 | Concrete notice categories (Emergency, circular, resolution) | §150.2: category OPEN | Adopted: GENERAL, EMERGENCY, CIRCULAR, RESOLUTION, MAINTENANCE (BRD §150.2 updated) |
| C-13 | mobile 41, 43 | Polls/AGM voting; visitor gate pass | Absent / explicit future scope | Both future scope (BRD §3.2 updated); no work items |

Material BRD-only gaps per screen (e.g. tenant-detail lifecycle actions `Create Lease`,
`Record Payment`, `Give Notice`, `Move Out`; expense `Edit`/`Mark Paid`/`Void`; work-order link
on expenses; `Export` on lists) are in the gap files and must appear in task acceptance criteria.
