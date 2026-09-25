# Stitch mobile mockups vs BRD — core screens (01–16)

Scope: `agentic/data/project-context/ui/stitch/mobile/{01..12,14..16}`. Screens 17–43 in
that folder (notice board, polls, visitor gate pass, etc.) are out of scope for this pass.

Cross-cutting finding, noted once: every mockup from 04 onward (My Buildings through Owner
Detail) renders the **same** bottom nav bar — Home / Units / Finance / Work / Community.
BRD §37 (line 1693) specifies **different** nav sets per role (generic admin-like:
Dashboard/Finance/Units/Work/More; Tenant: Home/Payments/Community/Profile; Owner:
Dashboard/Properties/Payments/Community/More; Committee: Dashboard/Finance/Work/Community/
More). None of the mockups match their role's BRD nav set exactly — flag before building
the shared shell nav.

No Flutter splash route or dashboard screens exist yet; `AppShell`
(`user_app/lib/app/shell/app_shell.dart`) is still the generic starter template (Home /
Explore / Account tabs, `_PlaceholderScreen` text) — not BRD-shaped.

---

### 01-splash-screen
- BRD: §39 (line 1752) | Work item: BOS-002/BOS-010 (identity) | Task/impl: none — `AppPages.initial` (`user_app/lib/app/routes/app_pages.dart:12`) is hard-set to `/login`; no splash route, no token/cached-user/active-building resolution exists.
- Shows: animated BuildingOS logo/halo, tagline, progress bar cycling through status strings ("Reconciling utility meters & BDT ledgers...", "Connecting to unit gateway...").
- Aligned: none to verify — it's a static decorative mockup, no real check-auth/resolve-building logic to compare.
- UI-only: the four rotating status messages are flavor text, not a literal step sequence — do not implement as real backend calls without approval.
- BRD-only: real auth-token check, cached-user load, active-building resolution, and the three-way navigation branch (single building → Dashboard; multi-building/pending/applications → My Buildings; logged out → Login) are all unimplemented — there is no splash screen in the app at all today.
- Open questions: should a splash route be added before Login now that F2/F4 give it data to resolve (buildings, applications, invitations)?

### 02-login-screen
- BRD: §40 (line 1773) | Work item: BOS-002/BOS-010 | Task(s): TASK-002 (phone OTP, COMPLETED), TASK-005 (locale toggle, COMPLETED) | Impl: `user_app/lib/features/authentication/presentation/pages/login_screen.dart`
- Shows: phone field with `+880` country code, "Continue with Phone" / "Continue with Google" buttons, EN/বাং toggle, Terms/Privacy links.
- Aligned: Phone + country code field, Continue with Phone button, Terms/Privacy links — all match §40's field/button/link list.
- UI-only: "Support" agent badge/link, "Owner, Tenant, Property Manager or Committee Member?" explainer card — decorative, not in BRD.
- BRD-only: none of §40's own fields are missing from the mockup; but FEATURE.md records Google sign-in as **deferred pending clarification** while the mockup still shows "Continue with Google" as a primary action.
- Open questions: is "Continue with Google" ready to build now, or should it stay hidden/disabled per the FEATURE.md deferral?

### 03-otp-verification-screen
- BRD: §41 (line 1793) | Work item: BOS-002/BOS-010 | Task(s): TASK-002, TASK-006 (429 rate-limit message, COMPLETED), TASK-007 (phone format) | Impl: `user_app/lib/features/authentication/presentation/pages/login_otp_verify_screen.dart`
- Shows: 6-digit OTP entry via custom keypad, countdown resend timer, "Edit Number", operator-detection badge, WhatsApp resend alternative.
- Aligned: 6-digit OTP field, Verify button, Resend OTP with countdown (expiration/retry/rate-limit rules — TASK-006 covers the 429 case), Change Number ("Edit Number").
- UI-only: "Resend via WhatsApp" alternate channel and "Operator matched (Grameenphone)" telco detection — not in §41; BRD only names SMS OTP.
- BRD-only: nothing structurally missing.
- Open questions: is WhatsApp-based OTP delivery in scope, or purely decorative in the mockup?

### 04-my-buildings-selector
- BRD: §42 (line 1810) | Work item: BOS-010 (F4) | Task: F4-T7 (Flutter buildings/invitations/units, COMPLETED) | Impl: `user_app/lib/features/units_ownership/presentation/pages/property_home_page.dart` (route `/my-buildings`, `property_pages.dart`)
- Shows: My Buildings cards (role, unit count, dues), My Properties aggregate, Pending Invitations (accept/decline), Building Applications status, Add Building / Join-with-code.
- Aligned: matches BRD's four sections (My Buildings, My Properties, Pending Invitations, Building Applications) and buttons (Open Building, Add Building; "Join with Invite Code" ≈ Join Building/Accept Invitation; "View Application").
- UI-only: specific BDT due amounts and combined "Building Manager & Owner" role badge — cosmetic; §42 line ~1847 explicitly requires property cards be derived from active Ownership records, not the OWNER role alone (F4-T4b's `/me/properties` is scoped to current allocations, consistent with this — worth confirming during QA).
- BRD-only: "Subscription/access warning when relevant and user is authorized to see it" (line ~1829) — not confirmed present in `property_home_page.dart` from the grep done here.
- Open questions: confirm the subscription/access-warning card is wired; confirm rent-due figures shown per building card aren't implied to be live yet (rent/finance is BOS-003/004, not BOS-010 F4 scope).

### 05-main-dashboard
- BRD: §43 (line 1854) | Work item: BOS-008 (role dashboards, not started per TASKS.md) | Task/impl: none — `AppShell._HomeTab` is a placeholder.
- Shows: role-agnostic cards (Total Outstanding, Rent Overdue, Maintenance, Collected This Month, Occupancy), Quick Actions row, urgent-announcement banner, upcoming-meeting card.
- Aligned: cards match §43's list (current dues/collected/outstanding/occupied-vacant/open maintenance/upcoming meeting/latest announcement); buttons match (Collect Rent, Add Expense, Create Work, Post Announcement→"Announcement", View Report→"Audit & Finance Report").
- UI-only: "System Mode: Property Lead Active" banner and specific BDT/percentage figures — decorative.
- BRD-only: nothing structurally missing from the card/button set itself.
- Open questions: mockup header role is "Building Lead" — not one of BRD's actor labels found in the sections read here; confirm which BRD role (§4) this maps to before building.

### 06-owner-dashboard
- BRD: §44 (line 1881) | Work item: BOS-008 | Task/impl: none.
- Shows: portfolio summary (rent collected/outstanding, maintenance due), units owned + occupancy, lease-expiry alert, per-unit performance list, recent bank payout.
- Aligned: cards (total units owned, rent collected, outstanding rent, maintenance due, occupancy) and buttons (View Properties, Rent Ledger, Maintenance, Statement/PDF) match §44.
- UI-only: "Recent Payouts & Transfers" bank-deposit card computing a net amount after a 3% management fee, and a multi-role "Switch" control — a management-fee deduction isn't in the ownership/rent rules read in this pass (§19–26 not fully covered here); do not implement that math without confirming it's a real business rule.
- BRD-only: none of §44's own items are missing.
- Open questions: confirm the 3% management-fee-on-payout rule exists anywhere in the financial sections before building it.

### 07-manager-dashboard
- BRD: §45 (line 1908) | Work item: BOS-008 | Task/impl: none.
- Shows: Due Today/Overdue/Collected Today cash cards, collection queue (bKash TrxID verify, cash record), overdue-tenant follow-up (call/WhatsApp/notice), move-in/out schedule.
- Aligned: cards (Due today, Overdue, Today's collections) and buttons (Collect Rent, Add Tenant, Create Lease, Record Move-Out→"Move-Out") match §45.
- UI-only: bKash-transaction verification workflow, "Send WhatsApp Reminder"/"Send Notice" channel-specific actions, move-in inspection checklist/reschedule — richer than §45's plain section list.
- BRD-only: none missing structurally.
- Open questions: confirm WhatsApp as an approved notification channel (§115 notification rules weren't read in this pass).

### 08-tenant-dashboard
- BRD: §46 (line 1929) | Work item: BOS-008 | Task/impl: none.
- Shows: current rent due (৳0, all clear), next due date, maintenance-included note, advance deposit, action row (Pay Rent/Schedule/History/Receipt/Lease/Contact Manager), building notices, latest receipt, lease summary.
- Aligned: cards (current rent due, maintenance if tenant-visible, next due date, advance balance) and buttons (View Payment History, Download Receipt, View Lease, Contact Manager) match §46.
- UI-only: "Report Issue" maintenance shortcut and a full lease-registration-ID display — reasonable extensions beyond §46's field list.
- BRD-only: none critical missing.
- Open questions: none material.

### 09-committee-dashboard
- BRD: §47 (line 1950) | Work item: BOS-008 | Task/impl: none.
- Shows: reserve fund/bank balance, billed/collected/outstanding MTD, monthly expenses vs budget, executive actions, expense-allocation ledger, governance resolutions requiring sign-off, AGM meeting card.
- Aligned: cards (maintenance collection, outstanding maintenance, monthly expenses, cash/bank totals, budget variance) and buttons (Add Expense, Generate Maintenance Fees, Create Work Order, Post Announcement, Create Meeting, Financial Report) match §47.
- UI-only: "Governance & Open Decisions" quorum-voting/sign-off workflow and "Remind President" action — a resolution/voting feature not itemized in §47 (closer to the separate `41-society-polls-agm-voting` mockup, which is outside this task's screen list).
- BRD-only: §47's "Work orders open" card isn't clearly present as its own tile (only implied via the expense ledger) — verify.
- Open questions: is the resolution/voting workflow part of BOS-007 (meetings) or new scope?

### 10-unit-list
- BRD: §48 (line 1970) | Work item: BOS-010 (F4) | Task(s): F4-T2/T3 | Impl: `user_app/lib/features/units_ownership/presentation/pages/building_units_page.dart`, `batch_units_page.dart`
- Shows: filter chips (occupancy/type/floor/status), unit rows with owner/tenant/rent/maintenance-due, "+ Add Unit".
- Aligned: filters (type, floor, occupancy, due status) and row fields (unit#, owner, tenant, occupancy, rent, maintenance due) match §48; "+ Add Unit" present; tap-row-to-detail pattern matches.
- UI-only: the rent/tenant/maintenance-due figures shown per row are BOS-003/004 (rental/finance) data — current `building_units_page.dart` only renders number/floor/type/area (confirmed by reading the file), so those columns are aspirational relative to F4's actual scope, not yet backed by an endpoint.
- BRD-only: explicit "Owner" filter chip, Search, and Sort controls aren't present in the current Flutter screen (only Refresh/Create-floor/Create-unit/Batch/Members actions were found).
- Open questions: confirm rent/tenant/dues columns wait for BOS-003/004 per BACKLOG.md's dependency chain before being added to this list.

### 11-add-edit-unit
- BRD: §49 (line 2000) | Work item: BOS-010 (F4-T2) | Impl: `user_app/lib/features/units_ownership/presentation/pages/unit_form_page.dart` (`UnitFormPage`, plus `FloorFormPage`)
- Shows: 4-step wizard — unit identification, ownership/allocation, occupancy & rental rules, utility meters — with owner search/assign, parking-slot counter, DPDC/Titas/WASA meter fields, a computed "Estimated Monthly Unit Inflow".
- Aligned: unit number, floor, unit type, area fields match §49; Save/Cancel present; validation (unique unit number case-insensitive, area >0 ≤2 decimals) matches F4-T2's scope and §49's validation rules.
- UI-only: the whole 4-step wizard framing, embedding ownership assignment inside unit creation (BRD keeps that as the separate §51 screen), parking-slot allocation, DPDC/Titas/WASA utility-meter fields, "Unit Compliance Check 85% Ready" gauge, and the inflow calculator — none of these appear in §49's field list, and the current `unit_form_page.dart` only implements number/floor/type/area/bedrooms/rate/notes/reason (i.e. it already matches §49, not the wizard).
- BRD-only: §49's "Save & Add Another" button isn't present in the mockup (only Cancel/"Save Unit").
- Open questions: confirm before building — are parking/utility-meter fields and inline owner-assignment approved scope expansions, or should this screen stay aligned to §49's plain field list?

### 12-unit-detail
- BRD: §50 (line 2026) | Work item: BOS-010 (F4-T2/T4a/T4b/T7/T8) | Impl: `user_app/lib/features/units_ownership/presentation/pages/unit_detail_page.dart` (currently minimal: number/floor/type/area/bedrooms/rate/notes + link into Ownership tab + Edit)
- Shows: header (unit#, Occupied/Vacant, type/sqft/floor, rent, maintenance chip), tabs (Overview/Ownership/Tenant/Lease/Rent & Dues/Maintenance/Documents/History), quick actions (Assign Owner, Add Tenant, Transfer Ownership, Mark Vacant, Create Lease, Log Maintenance, Collect Rent).
- Aligned: header fields (occupied/vacant, owner) and the full tab list closely match §50; buttons Assign Owner/Add Tenant/Transfer Ownership/Mark Vacant all appear in §50's button list.
- UI-only: the duplicated "Quick Actions" grid, bed/bath configuration, allocated-parking and utility-meter-ID display inside Overview — not itemized under §50 (§50 doesn't specify Overview-tab fields beyond the header).
- BRD-only: the current `unit_detail_page.dart` only renders Overview-like fields plus a link to the Ownership tab — Tenant/Lease/Rent/Maintenance/Documents/History tabs and the Add Tenant/Mark Vacant actions are not yet implemented (tenant/lease/rent are BOS-003/004, correctly out of BOS-010's scope per FEATURE.md).
- Open questions: none beyond the scope-sequencing already recorded in BACKLOG.md.

### 14-ownership-transfer
- BRD: §52 (line 2077), §127 (line 3925), §19.1 (line 1272) | Work item: BOS-010 (F4-T4a/T5b/T8) | Impl: `user_app/lib/features/units_ownership/presentation/pages/ownership_form_page.dart` (transfer mode) + `transfer_files_page.dart` (documents)
- Shows: 2-step legal conveyance flow — current-owner verification (NID/phone), new-owner selection/KYC, share % + effective date + "Transfer Instrument Type" dropdown, a legal/building-clearance checklist (maintenance cleared, deposit reconciliation, sub-registry deed upload), OTP-gated "Confirm & Authorize".
- Aligned: current owner, new owner, effective date, share transferred all present; the deed PDF upload matches BRD's "Document" field; registration/deed number ≈ "Transfer reference"; "Confirm & Authorize" satisfies §52's required confirmation modal; flow matches §127 (validate current ownership → new ownership record, immutable history per §19.1).
- UI-only: OTP-gated confirmation, the "Transfer Instrument Type" dropdown (Sale Deed / Inheritance-Hiba / Developer Handover), the maintenance-clearance + security-deposit-reconciliation checklist, and AGM-voting-share-update copy — none of these are in §52 or §127; do not implement without approval. The screen also displays a full NID number and partially-masked phone — verify against NID/personal-data handling rules before shipping as-is (not read in this pass).
- BRD-only: none of §52's own fields are missing.
- Open questions: is OTP-gated confirmation and instrument-type classification approved scope, or should the screen stay to §52's plainer field set? Confirm NID display/masking policy.

### 15-owner-list
- BRD: §53 (line 2101) | Work item: BOS-010 (F4) for membership; owner-directory financials are BOS-003/004/008 | Task/impl: no dedicated screen — closest existing piece is `user_app/lib/features/units_ownership/presentation/pages/members_page.dart` (building membership/invitation management: invite by phone, list/revoke members and invitations), which does not show per-owner unit/rent/dues data.
- Shows: searchable owner directory, units-owned count/list per owner, monthly rental value, resident vs non-resident/landlord tag, maintenance-dues badge, "+ Add New Owner".
- Aligned: Name, phone, number of units match §53; "+ Add Owner" present.
- UI-only: monthly rental value and maintenance-dues badges are financial data outside current scope; resident/non-resident classification, DESCO-meter and parking-bay tags — decorative extensions beyond §53's 5 listed fields.
- BRD-only: the existing `MembersPage` only supports membership/invitation admin, not an owner directory with units-owned, outstanding maintenance, or rent summary (§53's actual field set) — no equivalent screen exists yet.
- Open questions: will "Owner List" become a new screen combining `MembersPage` + F4-T4b's per-owner unit data, or is it deferred with BOS-003/004/008 financial data?

### 16-owner-detail
- BRD: §54 (line 2117) | Work item: BOS-010 (F4) for the profile portion; financial tabs are BOS-003/004/008 | Task/impl: none — no owner-detail page exists under `units_ownership/presentation/pages`.
- Shows: owner profile (phone/email/NID/e-TIN/verified badge), tabs (Units & Rent / Payment Ledger / Maintenance Dues / Documents / Activity), portfolio-yield summary, owned-units list with occupancy, recent financial transactions, document vault, "Call Owner" / "Generate Portfolio Statement".
- Aligned: sections match §54 (Profile, Owned units, Rent income≈Portfolio Yield, Maintenance liabilities, Documents, Activity).
- UI-only: full NID and e-TIN numbers displayed on-screen, payment-ledger/maintenance-dues figures (BOS-004 finance scope), document-vault download links — verify NID/personal-data display rules before implementing as shown (not read in this pass).
- BRD-only: §54's "Add Unit Ownership" and "Send Notification" buttons are not clearly present (mockup shows "Call Owner" instead of Send Notification); nothing is implemented yet on the Flutter side.
- Open questions: confirm NID/e-TIN display/masking policy; confirm "Add Unit Ownership" and "Send Notification" actions are still wanted on this screen.

---

## Screens in BRD §39–54 with no mockup

- **§51 Ownership Assignment Screen** (line 2056) — no `13-*` folder was exported for it (per the task brief). Its fields (Owner, Share %, Effective date, Notes; buttons Assign/Add Co-owner/Cancel; validation "total active ownership shares ≤ 100%") are the ones the transfer mockup (14) partially covers for the *transfer* case, but the plain "first assignment" screen has no corresponding mockup to compare.
