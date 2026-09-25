# Stitch Mobile Mockups vs BRD — Tenant/Lease/Finance (screens 17-33)

Scope: agentic/data/project-context/ui/stitch/mobile/17..33 vs BuildingOS_BRD_Agentic_Development.md §55-71 (screen specs, lines 2135-2534), domain rules §20-26 (1295-1501), §94 NID (3001), §108 Receipt Numbering (3276), §109 BD Localization (3294), §113 Financial Invariants (3471), §126 Collect Rent e2e (3857), §143 B/C (4346, 4360).

Work items: agentic/data/project-context/features/BOS-001/BACKLOG.md line 8-9. BOS-003 (rental) claims §55-62 (screens 17-24); BOS-004 (finance) claims §63-71 (screens 25-33). **Neither BOS-003 nor BOS-004 has a feature folder or task breakdown yet** (only BOS-001, BOS-002, BOS-010 exist under `agentic/data/project-context/features/`) — all "Tasks" below are "none yet."

---

### 17-tenant-list
- BRD: §55 Tenant List (line 2135) | Work item: BOS-003 | Tasks: none yet
- Shows: Tenant directory with occupancy stat card, per-tenant cards (name, flat, rent, lease status, paid/overdue state), call/chat/ledger actions.
- Aligned: Tenant/flat/rent/status data (line 2144-2149) present; `+ Add Tenant` (2151→"+ Register Tenant") present; search + filter icons present.
- UI-only (flag): "Verification Pending (bKash)" state with a `Verify` action for tenant-reported payments — no such manual-verification workflow exists in BRD's Collect Rent/allocation model (§22, §63, §126); "DPDC/WASA Active" utility badge and occupancy-rate card are not in BRD §55. Per-row `Reminder`/`chat`/`call` shortcuts are not listed in BRD's Buttons list (2151-2155) but don't contradict anything.
- BRD-only (missing): No `Export` button anywhere in the file (BRD 2155). Filter chips only show "All"/"Notice Period" (line 208-221 code.html) — BRD's "Moved out", "Rent overdue", and "Unit" filters (2138-2142) are not present as chips (may be hidden in an unread filter modal).
- Open questions: Is the bKash "Verification Pending" flow an intended manual-payment-claim feature for BOS-004, or should tenant-list stay read-only per §55/Backend "Rental + Reporting" (2157-2158)?

---

### 18-add-tenant
- BRD: §56 Add Tenant (line 2162) | Work item: BOS-003 | Tasks: none yet
- Shows: 3-step wizard, step 1 "Resident Profile": name/phone/email/NID+document upload/profession, emergency contact, family composition, unit assignment, DMP police verification panel.
- Aligned: Full name (required, code.html:188), Phone (required, 198), Email (210-212), Emergency contact (263-269) present.
- UI-only (flag — contradicts BRD): NID field is marked **required** (`*`, code.html:219) though §56 (2183) and §94 (3003) say "NID optional." NID value is rendered as **plain, unmasked text** (`19882691238479`, line 222) with no mask/blur anywhere in the file (grep found none) — contradicts §94 (3009) "Mask on UI by default." Also introduces NID document upload + OCR parsing and a "DMP Tenant Verification" (Dhaka Metropolitan Police CIMS) compliance panel (lines ~/text dump 165-175) — an entire police-verification domain concept with no counterpart anywhere in the BRD (checked §19-26, §94, §137-138 role/permission sections). "Profession & Organization" and "Resident Family Composition" (Total/Adults/Children breakdown) are extra fields beyond BRD's "Occupants count" (2171). Unit assignment is pulled into tenant creation step 1, ahead of BRD's separate Create Lease screen.
- BRD-only (missing): No "Permanent address" field (2170) — only Email Address exists (confirmed via grep, no address input). No "Photo optional" field (2172) — no photo/avatar upload input found. No "Notes" field (2173). No plain `Save Tenant` action — only `Cancel` / `Proceed to Lease Terms` (wizard-committed), so a tenant cannot be saved without also starting a lease, unlike BRD's two independent buttons (2176-2178).
- Open questions: Is DMP police verification an approved new requirement (BD statutory tenant registration with police) that should be added to the BRD/domain rules, or an unapproved invention? Given §94's explicit masking rule, the unmasked NID display should not be implemented as-is without approval.

---

### 19-tenant-detail
- BRD: §57 Tenant Detail (line 2187) | Work item: BOS-003 | Tasks: none yet
- Shows: Tenant profile header (photo, verified badge), contact/NID card, active lease summary, recent rent payments, document vault (DMP form, tenancy deed).
- Aligned: Tabs partially align — Overview/Lease & Agreement/Rent Ledger/Documents roughly cover BRD's Profile/Lease/Rent/Documents (2189-2195).
- UI-only (flag): NID shown unmasked again (`19822691238479`, no mask/blur found) — same §94 (3009) contradiction as screen 18. Adds a "DMP Police Form" tab not in BRD's tab list. "Issue Rent Receipt/Bill" action button is not in BRD's button list (2197-2202).
- BRD-only (missing): Tabs list is missing "Payments" and "History" entirely (BRD 2192, 2195; mockup only has Overview/Lease & Agreement/Rent Ledger/DMP Police Form/Documents). **All of BRD's core lifecycle buttons are absent**: no `Create Lease`, `Record Payment`, `Give Notice`, `Move Out` (grep for these strings returned nothing) — only `Edit`(icon)/`Share`/`Call Tenant`/`Issue Rent Receipt` are present (2197-2202 vs BRD).
- Open questions: Are the missing lifecycle actions (Give Notice/Move Out/Create Lease/Record Payment) deferred to other screens (e.g. Lease Detail, Collect Rent) by design, or an omission? BRD implies they belong on Tenant Detail directly.

---

### 20-lease-list
- BRD: §58 Lease List (line 2206) | Work item: BOS-003 | Tasks: none yet
- Shows: Lease cards (unit, lease #, tenant, landlord, term, rent, deposit) with renew/view-agreement/move-out actions; filter tabs.
- Aligned: Row data (Unit, Tenant, Start/End term, Rent) present (2220-2226). `Create Lease` button present (2217→2151 style).
- UI-only (flag): "Terminated" filter tab used instead of BRD's "Expired" (2212) — status-name mismatch. Each card exposes lease-specific actions (Renew, Initiate Move-out Settlement, View Agreement) not itemized in BRD's simple Actions list (2215-2218).
- BRD-only (missing): No `Export` action anywhere (grep found none, BRD 2218). No explicit "Notice" or "Unit" filter chips (BRD 2211, 2213) — only All/Active/Expiring Soon/Terminated tabs found.
- Open questions: Should "Terminated" and "Expired" be reconciled to one lease-state vocabulary (BRD lease states at line 1305-1315 list both `EXPIRED` and `TERMINATED` as distinct states — so the filter tab is actually just missing the other one, not a naming clash. Confirm both filters are intended.)

---

### 21-create-lease
- BRD: §59 Create Lease (line 2230) | Work item: BOS-003 | Tasks: none yet
- Shows: 4-step wizard, step 1 "Parties & Terms": unit/landlord/tenant, rent/service charge/deposit, term dates, special covenants (utility/sub-letting clauses).
- Aligned: Unit, Tenant, Start/End date, Monthly rent, Security deposit fields present (2233-2239, though labelled "Advance Security Deposit"). Backend note "Activation may trigger finance events" (2254) is consistent with a wizard that ends in deed generation.
- UI-only (flag): "Annual Escalation 10% on Renewal", "Lock-in Period", full legal covenant text (utility settlement, sub-letting prohibition, stamp-paper clause) — none of this is in BRD's Create Lease field list (2232-2244) or domain rules §20; it's a much heavier legal-contract model than the BRD specifies. Advance deposit shown as "৳1,500,000 / 3 Months Value" — worth checking this isn't conflating deposit with advance rent (§23 defines Advance Rent as a payment-ledger concept, not a lease-time deposit multiplier).
- BRD-only (missing): No `Save Draft` as a primary text button (only a small icon with title="Save Draft", code.html:156) and no `Save & Activate` button — main CTA is "Generate Lease Deed" (this is step 1 of 4, so Activate may appear on a later step not captured in this static mock). "Due day" and "Late fee policy" (2240-2241) are represented indirectly (Rent Due Calendar + Grace days) but no explicit late-fee-policy configuration found. "Parking charge"/"Other recurring charges" (2242-2243) not found as distinct line items (only "Building Service & Maintenance").
- Open questions: Confirm whether steps 2-4 (not in this file) contain Save Draft / Save & Activate and Agreement document upload (2244), since step 1 alone doesn't cover BRD's full field list.

---

### 22-lease-detail
- BRD: §60 Lease Detail (line 2258) | Work item: BOS-003 | Tasks: none yet
- Shows: Lease summary, parties, financial terms, timeline tracker (signed/start/notice-deadline/expiry), legal checkpoints (stamp duty, DMP verification, e-signatures).
- Aligned: Lease summary/Tenant/Unit/Rent schedule/Deposit sections present (2260-2267). "Download Agreement" present (2276 → "Download Signed Agreement (PDF)").
- UI-only (flag): "Legal Checkpoints" (stamp duty registration #, DMP verification, digital signatures) is a new compliance layer not in BRD §60 or §20. "Send Renewal Notice" is a distinct concept from BRD's plain `Renew` button (2275).
- BRD-only (missing): No explicit `Activate` / `Edit Draft` buttons (BRD 2271-2272) — acceptable since this instance is already ACTIVE. No `Terminate` button by that name — only "Initiate Early Termination / Vacating" (functionally equivalent but reworded). No "Documents" or "Invoice history" section visible (BRD 2267-2268) beyond the single "Download Signed Agreement" link.
- Open questions: None significant; mostly renaming/state-dependent omissions.

---

### 23-rent-overview
- BRD: §61 Rent Overview (line 2280) | Work item: BOS-003 | Tasks: none yet
- Shows: Monthly rent-collection dashboard (receivable/collected/outstanding), per-unit row list with paid/overdue/verification-pending states, bank escrow sync panel.
- Aligned: `Collect Rent` and `Export PDF` buttons present (2301, 2303). Filter/search present.
- UI-only (flag): **"Advance balance total" card is missing** (2286) — cards shown are Total Receivable/Collected/Outstanding only, no advance-ledger figure anywhere on the dashboard, despite §23 (1404-1419) making advance a first-class ledger balance. Adds a "Prime Bank PropTech Escrow" auto-reconciliation panel and a per-tenant "Verification Pending (bKash)" + `Verify` row action — neither concept (bank escrow auto-sync, tenant self-reported payment awaiting manager verification) exists in BRD's Finance model (§21-23, §126).
- BRD-only (missing): Tabs are All/Paid/Overdue/Partial — BRD's "Due" tab (2289) is replaced by "All" rather than being a distinct due-only view. No explicit `Generate Invoices` button (2302) found in the extracted text — only Collect Rent/Bulk Remind/Export PDF/Sync.
- Open questions: Where does the advance-balance total surface if not here? Is the escrow/auto-reconciliation feature an approved BOS-004 scope addition (platform-held funds) — this has real financial-architecture implications (see 25-collect-rent below) and should be confirmed before backend design.

---

### 24-rent-invoice-detail
- BRD: §62 Rent Invoice Detail (line 2310) | Work item: BOS-003 | Tasks: none yet
- Shows: Fully-paid invoice with itemized charges, transaction ledger, and an automated payout split (Landlord payout vs Building Maintenance Fund).
- Aligned: Invoice no, billing period, tenant, unit, charges (base rent/service charge), paid, outstanding all present (2313-2320).
- UI-only (flag — significant): "Automated Ledger Allocation" splits the payment into **"Landlord Payout (Less 3% fee) ৳48,500"** and **"Building Maintenance Fund ৳5,000"**. A 3% platform-fee deduction and automatic landlord disbursement is **nowhere in the BRD** — §22 Payment Allocation Model (1357-1401) only allocates a payment across invoices/advance ledger, with no mention of a platform commission or fund-splitting/payout mechanism. This is a materially different financial model from what's specified and should not be implemented without explicit approval. "Discounts" (2318) and an "Allocations" list (2321, i.e. which invoices this payment touched) are not shown — this screen shows only a single fully-settled invoice, no multi-invoice allocation view.
- BRD-only (missing): No `Add Adjustment` or `Record Payment` buttons (2323-2327) — acceptable given invoice state is PAID, but no BRD-specified alternative for un-paid state was captured in this mock. No `Send Reminder` (2327) — replaced by "Send Receipt to Tenant via WhatsApp/SMS."
- Open questions: Confirm the 3% platform fee / payout-split concept with the business owner — if real, it needs new BRD domain rules (fee schedule, payout timing, ledger entries) before any BOS-004 task is written.

---

### 25-collect-rent
- BRD: §63 Collect Rent (line 2331) | Work item: BOS-004 | Tasks: none yet
- Shows: Single-invoice payment capture (amount/date/method/reference/payer phone), a "Fund Allocation Split" breakdown, receipt-screenshot upload, notes.
- Aligned: Amount received, Payment date, Method (bKash/Nagad/Bank/Cash/Cheque — matches §109 BD localization, line 3301), Reference, Notes, Receipt attachment all present (2333-2342).
- UI-only (flag — significant): Same platform-fee/fund-split concept as screen 24: "Base Rent to Landlord ৳47,000", "Building Maintenance Fund ৳5,000", "Sinking Reserve Fund", "Auto-deduct 3% platform fee ... disbursed to BuildingOS Platform Treasury." This is the point where the fee model is actually captured at collection time — again, no BRD basis (§22/§23/§126 e2e flow, 3857-3921, describes create-payment → create-allocation → create-advance → receipt, with no fee/disbursement step). Flag strongly: if built as shown, it would materially diverge from §113 invariants' implicit "allocations map only to invoices/advance" model.
- BRD-only (missing): No **invoice selection list** ("Selected invoices", 2336) — the screen pre-fills one lump overdue balance rather than letting the user pick/allocate across multiple open invoices. No **"Auto allocate oldest first / Manual allocation toggle"** (2345-2346) UI. No **confirmation summary** (Received/Allocated/Advance created/Remaining due, 2354-2357) — replaced by the fund-split summary instead. Only one combined submit button ("Confirm & Record Payment") rather than distinct `Save Payment` / `Save & Share Receipt` (2349-2350).
- Open questions: Same as #24 — is fee-splitting in scope? Also: how does this screen support Scenario B/C (partial rent / advance rent, §143 lines 4346-4370) without an invoice-selection or allocation UI? As drawn, it can't demonstrate "allocates 20,000, invoice PAID, advance ledger 30,000" (line 4366-4368) since there's no multi-invoice or advance-consumption UI.

---

### 26-payment-detail
- BRD: §64 Payment Detail (line 2366) | Work item: BOS-004 | Tasks: none yet
- Shows: Receipt view with amount, payer, collector, method, ledger allocation (rent + CAM + net landlord credit), digital signature/QR verification.
- Aligned: Receipt number, Payer, Amount, Method, Time, Collector, Audit reference (as "Verified Hash"/timestamp) present (2368-2376). `Reverse Payment` present as "Request Reversal / Refund," and correctly **no delete action** anywhere (matches §113 line 3479 "Delete operation for finalized payment is forbidden" and BRD 2383 "No delete").
- UI-only (flag): Receipt number shown as `RCP-2025-0302` — does **not** match §108's server-generated format `BLD01-RNT-202609-000123` (building code + category + year/month + sequence, lines 3282-3290). No building-code or category segment is present in the mockup's receipt number. Repeats the "Net Landlord Credit" payout-split concept from screens 24/25. Adds QR-code verification and a cryptographic hash display not in BRD.
- BRD-only (missing): "Invoice allocations" (2375, i.e. list of invoices this payment settled) is not shown — only a single "March 2025" ledger allocation line, no multi-invoice breakdown.
- Open questions: Confirm intended receipt-number format before backend numbering (§108) is implemented, since the mock's format would need to change to comply.

---

### 27-payment-reversal
- BRD: §65 Payment Reversal (line 2387) | Work item: BOS-004 | Tasks: none yet
- Shows: Reversal request form (full/partial type, reason dropdown, audit note, attachment) with a two-party "Dual-Signoff Protocol" gate.
- Aligned: Reason (dropdown) + Notes (Explanation/Audit Note) present (2389-2391). Original payment / impact-on-balance shown in the warning banner (2393-2396, "re-open tenant's overdue balance of ৳55,000 ... debit the building ledger"). `Confirm Reversal`/`Cancel` present as "Submit Reversal Request"/"Cancel & Keep Receipt" (2398-2400). Requires confirmation and permission (2402) — satisfied via the dual-signoff panel.
- UI-only (flag): "Partial Adjustment" reversal type is not in BRD §24 (1422-1436), which only models a single REVERSED status/full reversal — partial reversal would need its own domain rule (does it create a negative allocation? adjust invoice due? not specified). "Dual-Signoff Protocol" (Committee Secretary + Building Lead approval before ledger deduction) is a governance layer beyond BRD's "Require confirmation and permission" (2402) — introduces an approval workflow/roles not defined for this action anywhere in §4-5 role model as reviewed.
- BRD-only (missing): "Advance effect" (2396) is not explicitly itemized (only invoice/balance impact is shown) — if the original payment had created an advance-ledger credit, the mockup doesn't show how/whether that's clawed back.
- Open questions: Is partial reversal an approved requirement? If so it needs a §24 rule update (partial reversal semantics, allocation-reversal math) before BOS-004 tasks are written; per §113 invariant 4 ("Reversed payment cannot be allocated") a partial-reversal design must avoid leaving the original payment "REVERSED" while still partially allocated.

---

### 28-maintenance-overview
- BRD: §66 Maintenance Fee Dashboard (line 2406) | Work item: BOS-004 | Tasks: none yet
- Shows: Assessed/collected/treasury dashboard with per-unit invoice rows, tabs, association bank sync panel.
- Aligned: `Generate Monthly Fees` and `Record Payment` present (2419-2421, as "Generate Monthly"/"Record Payment"). Billed/Collected/Outstanding figures present in substance (2409-2411).
- UI-only (flag): "Treasury Balance" and "Reserve Sinking Fund" cards, plus a live bank-account reconciliation panel ("Trust Bank Society Account... Live Sync"), are new building-treasury concepts absent from §25 Maintenance Fee Rules (1439-1462) and §113. "Exempt / Retained" tab/status is not one of BRD's defined states (BRD doesn't define a maintenance-invoice status enum at all, so this is an assumption, not a contradiction, but should be confirmed).
- BRD-only (missing): No explicit `Collection %` card (2412) — a "80.5% Done" progress figure substitutes informally. **No `Export Report` button** (2422) — the third action slot is instead a shortcut to "Expense Register," not an export.
- Open questions: Is the building having its own bank/treasury/reserve-fund ledger an approved BOS-004 (or later phase) feature? No domain rule currently defines building-level treasury accounts.

---

### 29-generate-maintenance-fees
- BRD: §67 Generate Maintenance Fees (line 2426) | Work item: BOS-004 | Tasks: none yet
- Shows: Assessment wizard — billing month/due date, rate model (fixed/area/tiered/custom), itemized budget breakdown, unit scope, notification dispatch toggles.
- Aligned: Billing month, Due date, Unit scope ("Target Unit Selection"), Rate policy (Assessment Model) all present (2428-2432). Exclusions covered via "Developer Ledger Inclusion" toggle (2433).
- UI-only (flag): "Itemized Budget Allocation" (6 named operating-cost line items feeding the total) is a level of budget detail not in BRD's simple field list (2428-2437) — reasonable extension, but not specified. Auto SMS/WhatsApp notification dispatch tied into fee generation (2432 doesn't mention notification, though §115 Notification Rules may cover it generally — not read in depth here).
- BRD-only (missing): No distinct **`Preview`** step/button (2440) — only "Generate & Dispatch 36 Invoices" and "Cancel Assessment Run"; the itemized budget list acts as an implicit preview but there's no explicit Unit×Amount preview table (2436-2437) shown before commit. No visible warning enforcing "prevent duplicate generation for same billing period" (2444) — likely a backend-only concern, but worth a UI confirmation dialog per BRD's emphasis.
- Open questions: Confirm whether "Preview" and "Generate" should be two distinct user actions (per BRD) rather than one combined dispatch button, especially given this issues 36 real invoices with SMS/WhatsApp notifications immediately.

---

### 30-maintenance-payment
- BRD: §68 Maintenance Payment (line 2448) | Work item: BOS-004 | Tasks: none yet
- Shows: Payment capture for one owner/unit's maintenance dues (current + arrears + waived late fee), method, destination account, attachment, note.
- Aligned: Owner/Payer, Unit, Invoice(s) (current + arrears shown as an allocation breakdown), Amount, Payment method (bKash/Nagad/Bank/Cash/Cheque), Reference, Date all present (2450-2457). Buttons roughly match `Save Payment`/`Save & Receipt` via "Confirm & Issue Association Receipt."
- UI-only (flag): "Deposited To" association-account selector (choice of Trust Bank / BRAC Bank / Petty Cash) is a new fund-destination concept, consistent with the treasury theme flagged in screen 28 but not in §25/§68.
- BRD-only (missing): None major — this screen aligns reasonably well aside from the treasury/account-destination addition.
- Open questions: Same treasury-account question as screen 28.

---

### 31-expense-register
- BRD: §69 Expense List (line 2468) | Work item: BOS-004 | Tasks: none yet
- Shows: Monthly expense list with budget-burn cards, category spend breakdown, status tabs, vendor P&L link.
- Aligned: Filters conceptually covered by tabs (Status); cards roughly cover "This month"/"Approved"/"Pending" (2477-2481, though relabeled/expanded).
- UI-only (flag — contradicts BRD status enum): Row statuses shown are **"Approved & Paid"** and **"Settled"** — neither matches §26's defined Expense statuses `DRAFT/SUBMITTED/APPROVED/REJECTED/PAID/VOID` (1486-1495). These need to be mapped to (or reconciled with) the canonical enum before implementation, otherwise the UI implies status values the backend model doesn't have. "Budget: ৳2,34,000 / 76.2% burned," "Operational Runway Left," and "Sinking Reserve Intact" cards are new budget-tracking concepts not in §26 or §69.
- BRD-only (missing): No distinct Date/Category/Vendor/Work-order filters visible in extracted text (only a generic filter icon + status tabs) — BRD lists five filter dimensions (2470-2475); can't confirm the filter modal's contents without opening it, flagged as unconfirmed.
- Open questions: Confirm canonical status vocabulary mapping ("Settled"/"Approved & Paid" → PAID?) before backend/UI wiring.

---

### 32-add-expense-voucher
- BRD: §70 Add Expense (line 2489) | Work item: BOS-004 | Tasks: none yet
- Shows: Expense voucher form — title/category/amount/date/vendor/payment method/attachment, with a rule-driven dual-signoff routing panel for amounts over ৳10,000.
- Aligned: Title, Category, Amount, Date, Vendor, Method, Description, Receipt photo/document all present (2492-2500). Buttons `Save Draft`/`Submit`/`Cancel` present (as Save as Draft/Submit Voucher for Approval/Discard).
- UI-only (flag): "Trade License / BIN" (vendor tax ID) field and the concrete "Committee Treasurer + Building Lead dual-signoff" named-approver routing go beyond §26's generic "Approval workflow can be building-configurable" (1497) — not contradicting, but a specific implementation the BRD doesn't specify. Receipt/invoice attachment is marked **"Mandatory"** here though BRD doesn't explicitly state optionality for this field (only Work order is flagged optional at 2498) — low-risk, just worth confirming.
- BRD-only (missing): **No "Work order" field at all** (2498, "Work order optional") — cannot link an expense to a work order from this screen, unlike BRD's explicit (optional) field.
- Open questions: Should Work-order linkage be added back for BOS-006 cross-linkage (expense↔work order, per BACKLOG line 10 "BOS-004 expense linkage")?

---

### 33-expense-detail
- BRD: §71 Expense Detail (line 2512) | Work item: BOS-004 | Tasks: none yet
- Shows: Expense voucher detail with a 3-stage "Dual Sign-off Tracker" (Estate Manager → Committee Treasurer → Society Secretary), itemized bill breakdown, vendor/payment info.
- Aligned: Amount, Status (Pending Committee Signoff), Vendor, Created by ("Initiated By"), Approved by (Treasurer entry), Receipt, all present (2514-2521). Sign-off history with timestamps satisfies "All status transitions audited" (2530).
- UI-only (flag): The named 3-role sequential sign-off tracker is a specific workflow implementation beyond §26/§71's generic model — same caveat as screen 32.
- BRD-only (missing): **No `Edit`, `Mark Paid`, or `Void` buttons** (2524, 2527-2528) — only "Approve & Authorize Disbursement" and "Reject or Request Audit Clarification" are present. "Related work" (work order link, 2521) is absent, consistent with screen 32's missing Work order field.
- Open questions: Are Edit/Mark Paid/Void state-dependent (hidden because this voucher is mid-approval), or omitted from the design entirely? Needs confirmation since BRD requires all five actions to exist somewhere in the expense lifecycle.

---

## Screens in BRD §55-71 with no mockup

None. All 17 BRD screen specs from §55 Tenant List through §71 Expense Detail have a corresponding folder (17-tenant-list … 33-expense-detail), one-to-one in order.

## Cross-cutting flags (highest priority)

1. **Platform fee / fund-split model (screens 24, 25, 26)**: mockups introduce an automatic 3% platform-fee deduction and landlord/maintenance-fund payout split at payment time. This has no basis anywhere in the BRD's Payment Allocation Model (§22), Advance Rent (§23), Financial Invariants (§113), or the Collect Rent e2e spec (§126) — it's a new financial architecture, not a UI nuance, and should not be implemented without explicit product/business approval and a corresponding BRD/domain-rule update.
2. **NID handling contradicts §94 (screens 18, 19)**: NID is marked required (BRD says optional) and displayed unmasked in plaintext in two screens, directly contradicting "Mask on UI by default" (line 3009).
3. **DMP (Dhaka Metropolitan Police) tenant-verification workflow (screens 18, 19)**: an entire regulatory-compliance feature with no counterpart in the 150-section BRD — needs explicit scoping before any backend work.
4. **Receipt number format mismatch (§108, screens 19, 26)**: mockups show `RCP-2025-0302` / `#RCP-2025-0302`, not the specified `BLD01-RNT-202609-000123` (building code + category + year-month + sequence).
5. **Missing core lifecycle actions on Tenant Detail (screen 19)**: `Create Lease`, `Record Payment`, `Give Notice`, `Move Out` are all absent, though BRD §57 lists them as primary actions on this screen.
