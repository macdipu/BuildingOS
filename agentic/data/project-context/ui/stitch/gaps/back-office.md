# Stitch back-office UI vs BRD comparison

Sources: BRD §149 (lines 4598-5360), §90 (2894), §114 (3486), §8.10/§8.12 (786/802),
§110 (3309). Tasks: BOS-010 back-office-console REQUIREMENTS.md (BOC-01..09),
DELIVERY-PLAN.md (F6-T1..T10).

---

### 01-multi-tenant-platform-overview-metrics
- BRD: §149.18 (5224), §149.2 (4626) | BOC-09, BOC-01/02 | Task: F6-T9 (also F6-T6 nav)
- Shows: Sidebar nav (flat: Platform Overview, Buildings Pipeline, Subscriptions &
  Billing, System Audit & Security, User Entitlements, API & Webhooks, Settings,
  Documentation, Support) + a dashboard of society/unit counts, a large "Monthly
  Platform GMV" / "SaaS ARR / Platform MRR" panel, live payment-gateway transaction
  feed (bKash/Nagad/Trust Bank), regional cluster health map, and platform alerts.
- Aligned: "Total Active Societies 148" / "Suspended (2)" roughly cover the
  §149.18 "total buildings by lifecycle state" / "active/suspended buildings"
  metrics (5228-5231). Header "Audit Log" link maps to System > Audit Logs
  (BOC-08).
- UI-only (not in BRD; conflicts with §149.23 MVP boundary at 5339-5360):
  - "Monthly Platform GMV ৳38.4 Crore", "SaaS ARR/MRR ৳24.8 Lakh", live
    "Real-Time Transactions & Gateway Throughput" (bKash MFS API, NPSB/BEFTN,
    Nagad Business rails), and a per-transaction ledger table with escrow
    settlement states. §149.18 (5237) explicitly says "Financial SaaS revenue
    metrics may be added once automated billing/provider integrations exist" —
    MVP explicitly excludes automatic charging/collection (5353-5354).
  - "DMP Police Verification service" / RAJUK/DMP compliance banner, "Escrow
    Multi-Sig Integrity" — no such integration anywhere in the BRD.
  - Flat single-level nav with "API & Webhooks", "Settings", "Documentation",
    "Support" (top-level) items not in §149.2's nav tree at all.
- BRD-only (missing from mockup):
  - §149.18's own recommended metrics are almost entirely absent: applications
    awaiting review, buildings in onboarding, subscriptions by plan/status,
    trials ending soon, onboarding cases requiring action, active support
    sessions (5229-5235) — none of these appear; the dashboard is financial-
    metric-first instead.
  - Full §149.2 nav hierarchy: no "Users" section (Platform Users/Building
    Admins/Onboarding Agents/Support Agents, BOC-05) and no "Support" section
    (Assisted Onboarding/Active Support Sessions/Support History, BOC-06/07)
    anywhere in the sidebar.
- Open questions for operator: Is the GMV/MRR/live-transaction panel intended
  as a future phase (post-MVP billing integration) or should it be removed from
  the back-office MVP scope entirely? Should "API & Webhooks"/"Settings"/
  "Documentation" become real nav items (none are in requirements/tasks)?

### 02-building-onboarding-pipeline
- BRD: §149.3-149.7 (4664-4801), §149.4 state machine (4713), §149.5 review
  screen (4744), §149.6 duplicate detection (4773), §149.9 onboarding flow
  (4815) | BOC-03, BOC-06 | Task: F6-T7 (Buildings screen), F6-T3/T8 (assisted
  onboarding, not visibly represented here)
- Shows: A 4-stage kanban ("New Applications", "Deed & Legal KYC",
  "Configuration & Staging", "Live & Production") with per-stage counts, a
  filterable building list/table, and a "Quick Building Review" detail panel
  with legal-deed/RAJUK verification checklist, escrow bank account link, and
  "Approve & Issue Production Keys" / "Request Clarification" / "Reject
  Application" actions.
- Aligned: Building list uses holding number + address/area/district fields,
  consistent with §149.3's `BuildingApplication` fields (4678-4709) and the
  duplicate-detection signals in §149.6 (building name, address, holding
  number, 4777-4780). Reject / request-more-info / approve actions map to
  §149.5's `Reject` / `Request More Information` / `Approve` (4763-4766).
- UI-only (not in BRD; potential conflicts):
  - The 4-stage kanban ("New Applications/Deed & Legal KYC/Configuration &
    Staging/Live & Production") does not match §149.4's state machine
    (DRAFT/SUBMITTED/UNDER_REVIEW/MORE_INFORMATION_REQUIRED/REJECTED/APPROVED/
    ONBOARDING/ACTIVE, 4713-4733) or §149.10's onboarding statuses
    (NOT_STARTED/IN_PROGRESS/WAITING_FOR_CUSTOMER/READY_FOR_ACTIVATION/
    COMPLETED, 4968-4976) — it invents its own taxonomy.
  - Government "Sub-Registry" / RAJUK deed clearance verification, "Bank
    ESCROW Account Connection", dedicated per-building sub-domain
    (`greenvalley.buildingos.bd`) and "Dedicated DB Tenant" — none of this
    government-registry / per-tenant-database model appears in the BRD
    (§110.2 describes Building as SaaS customer account, not a dedicated DB
    or subdomain per tenant).
  - "Approve & Issue Production Keys" wording differs from and adds scope
    beyond the plain `Approve` action of §149.5/§149.7 (production key
    issuance is not a BRD-defined step).
- BRD-only (missing from mockup):
  - §149.5's required review-screen fields/actions not shown: applicant
    identity/contact, applicant relationship to building, verification
    documents list, submission date, **review history**, **internal notes**,
    **possible duplicate-building matches** panel, `Assign Onboarding Agent`,
    `Add Internal Note`, `Open Applicant Profile` actions (4746-4769).
  - §149.9's 11-step guided onboarding flow (Building Info -> Structure/Floors
    -> Units -> Owners -> Committee & Staff -> Maintenance Config -> Rent
    Config -> Payment Methods -> Invitations -> Review -> Activate,
    4817-4831) is not represented; "Configuration & Staging" only mentions
    "Unit mapping, meter allocation & bank account sync".
  - No assisted-onboarding-agent assignment/session UI (BOC-06, §149.11).
- Open questions for operator: Should the kanban stage model replace or sit
  above the BRD state machine (i.e. is it a display grouping over the real
  statuses, or a separate/conflicting workflow)? Is RAJUK/sub-registry deed
  verification an approved future integration, or out of scope entirely (it
  is not mentioned anywhere in §149 or REQUIREMENTS.md)?

### 03-society-subscriptions-billing
- BRD: §149.15 subscription domain (5096), §149.16 entitlements (5158),
  §149.23 MVP boundary (5339) | BOC-04 | Task: F6-T7
- Shows: Revenue KPI row (gross SaaS revenue, billed unit licenses, collection
  rate, delinquent accounts), 3 plan cards (Starter/Professional/Enterprise)
  with per-unit pricing and feature checklists, and a per-building billing
  table (plan, units/rate, monthly amount, next renewal, payment rail,
  invoice status, actions).
- Aligned: Plan cards' name/price/feature-list structure is consistent with
  §149.15's `SubscriptionPlan` (code/name/enabled_features, 5101-5113).
  Billing-status filter options ("Active & Paid", "Overdue/Grace Period",
  "On-boarding Trial", "Canceled/Suspended") loosely track the
  §149.15 subscription states TRIAL/ACTIVE/PAST_DUE/GRACE_PERIOD/SUSPENDED/
  CANCELLED (5138-5145).
- UI-only (directly conflicts with §149.23 MVP boundary, 5351-5359 — "MVP does
  not require: automatic card charging, bKash/Nagad subscription collection,
  invoices for BuildingOS SaaS fees, tax/VAT automation, dunning automation"):
  - "Subscription Collection Rate 97.8% Auto-settled", "bKash Auto-pay",
    "Trust Bank Direct Debit", per-building "Escrow Sync ID", VAT-inclusive
    invoice numbers (`#INV-SaaS-2025-089`), "Delinquent Accounts" overdue-amount
    tracking, and "Grace Period (Day 4/7)" automated countdown — all describe
    automated billing/collection the MVP explicitly excludes.
  - "Download MRR Ledger (Excel)" — a revenue-ledger export not in
    REQUIREMENTS.md/BOC-04 scope.
- BRD-only (missing from mockup):
  - No visible "Trials" section/list distinct from the billing-status filter
    (§149.2 nav requires a "Trials" nav item; §149.15 back-office actions
    include start/extend/end trial, 5149).
  - Back-office actions required by §149.15 (5147-5156) not evidenced in the
    text: `assign/change plan`, `suspend/reactivate`, `cancel`, `inspect
    subscription history`, `inspect effective entitlements` — table only shows
    generic "upgrade"/"receipt"/"description" icon actions.
  - §149.16 entitlement keys (rent_management.enabled, max_units, max_users,
    storage_limit_mb, support_tier, etc., 5162-5174) are not shown as
    per-plan configurable entitlements, only marketing-style feature bullets.
- Open questions for operator: Is the automated-billing UI a placeholder for a
  future phase after live billing integration, and should it be suppressed
  until then per §149.23? Should Trials get their own nav-level list to match
  §149.2, separate from the billing-status filter?

### 04-system-audit-security-log
- BRD: §90 audit log screen (2894), §114 audit requirements (3486), §149.21
  (5312), §8.10 audit service (786) | BOC-08 | Task: F6-T5, F6-T9
- Shows: Compliance KPI row (security posture score, ledger integrity, DMP
  sync rate, active admin sessions), filters (date range, severity, building,
  quick-filter categories), an "Immutable Cryptographic Audit Trail" table
  (timestamp+hash, initiator/role, event category, target entity, IP/network,
  ledger status, action), and a "Statutory Compliance & Regulatory
  Checkpoints" panel (Bangladesh Bank, DNCC bylaw, Digital Security Act).
- Aligned: Table columns Initiator/Actor, Event Category/Action, Target
  Entity, Timestamp roughly cover §90's required Actor/Action/Entity/
  Timestamp (2905-2912). One logged event ("Attempted Root Role
  Self-Assignment... Authorization Denied") conceptually matches the security
  rule in §149.22 that "Building Admins cannot grant themselves platform
  roles" (5331), and "Financial audit records cannot be deleted" (2914) is
  consistent with the immutable/append-only framing.
- UI-only (not in BRD):
  - "Cryptographic ledger integrity"/Merkle-root/SHA-256 hash-chain framing,
    "Generate Compliance Certificate", "DMP Police Verification Sync",
    per-event IP address/ISP/device fingerprint, and the "Statutory
    Compliance & Regulatory Checkpoints" panel (Bangladesh Bank circular,
    DNCC bylaw, Digital Security Act certification) — none of this exists in
    §90, §114, or §149.21; the BRD's audit model is a plain queryable log,
    not a blockchain-style ledger with regulatory certification.
- BRD-only (missing from mockup):
  - §90's required filter "User" (by actor) and "Module" are not present —
    mockup filters by Severity, Building, and a fixed quick-filter category
    list (Critical/Financial/Config/DMP Sync) instead of Module; no
    entity-type filter either.
  - §90's "Old summary" / "New summary" before/after change columns
    (2909-2910) are not shown in the table (only a "Details" link, contents
    unknown from HTML text).
  - §114/§149.21 required audited-action categories not evidenced as
    distinct log entries: role changes, ownership transfer *approval* vs.
    transfer itself, lease activation/termination, payment reversal,
    expense approval, meeting-decision edits (3488-3502, 5314-5324) — sample
    rows only show a utility-split recalculation, a DMP sync, a blocked
    privilege escalation, an ownership-transfer record, and a bKash token
    refresh.
- Open questions for operator: Should the "cryptographic ledger"/Merkle-tree
  presentation be treated as a UI skin over the plain immutable audit log in
  §8.10 (i.e. cosmetic only), or does it imply a different (blockchain-backed)
  storage design that isn't in TECH-SPEC.md? Are the regulatory-compliance
  certificates (Bangladesh Bank/DNCC/Digital Security Act) an approved future
  compliance feature or out of scope?

---

## Navigation gaps

§149.2 (4626-4662) nav items with **no corresponding mockup screen** among the
4 screens reviewed:

- **Users** section entirely: Platform Users, Building Admins, Onboarding
  Agents, Support Agents (BOC-05, F6-T2/T8).
- **Support** section entirely: Assisted Onboarding, Active Support Sessions,
  Support History (BOC-06/07, F6-T3/T4/T8).
- **Buildings** sub-items as named in the BRD: Applications / Under Review /
  Onboarding / Active Buildings / Suspended Buildings / Archived Buildings —
  screen 02 uses a differently-named 4-stage kanban instead of these six
  states, so none map 1:1; "Archived Buildings" (explicitly a stub per
  BOC-03) has no representation at all.
- **System** sub-items: Platform Settings, Feature/Entitlement Configuration,
  System Health — only "Audit Logs" (as "System Audit & Security") is
  represented; the mockups' generic "Settings" and "User Entitlements" items
  are not clearly the same as the BRD's named sub-items.
- **Dashboard** as the single top nav label — mockup calls it "Platform
  Overview" (naming-only difference, not a gap).
