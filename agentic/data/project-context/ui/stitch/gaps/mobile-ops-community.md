# Stitch Mobile UI vs BRD — Operations Screens (34-43)

Source: `agentic/data/project-context/BuildingOS_BRD_Agentic_Development.md`
Backlog: `agentic/data/project-context/features/BOS-001/BACKLOG.md`

---

### 34-report-issue
- BRD: §27 Work Order Rules (line 1501-1538), cf. §73 Create Work Order (line 2554) | Work item: BOS-006 | Tasks: none yet
- Shows: resident-facing issue intake — unit/room picker, issue category (plumbing/electrical/HVAC/carpentry/civil/lift-generator), 4-tier urgency (Low/Medium/High/Critical), description + tags.
- Aligned: category set maps to BRD's work-order categories (generator, pump, lift, plumbing, electrical, structural — line 1505-1513); priority levels map to BRD `LOW/MEDIUM/HIGH/CRITICAL` (line 1533-1538); description/attachments map to BRD fields (line 1518-1528).
- UI-only (not in BRD — do not implement without approval): room/segment-level location targeting ("Flat 4A – Master Bathroom"), hashtag issue-tags, SLA-hours framing on urgency tiers, "Pre-verified unit ledger" badge.
- BRD-only: BRD's work order fields include `contractor_id`, `estimated_cost`, `planned_start` — not present on this intake screen (expected, since resident doesn't set these; consistent with §73 being the staff-side create screen).
- Open questions: is this a distinct resident-facing screen from §73 Create Work Order, or the same use case with a resident-restricted field set? BRD doesn't specify a separate "report issue" screen.

### 35-work-order-register
- BRD: §72 Work Order List Screen (line 2534-2552) | Work item: BOS-006 | Tasks: none yet
- Shows: ticket queue with SLA countdowns, KPI tiles (active/urgent/SLA%), per-ticket assignee/status, "Call Tech"/"Track Progress" actions.
- Aligned: filters concept (status/priority/contractor/due date, line 2536-2540) and `+ Create Work`/Filter/Search buttons (line 2547-2550) are present in spirit ("+ New Ticket", search, tune/filter icon).
- UI-only (not in BRD — do not implement without approval): building-level KPI tiles (Active/Urgent/SLA compliance %), "Technician Roster" shortcut, chargeable-vs-common cost tag ("Resident Chargeable • Est: ৳1,800"), direct "Call Tech" action from the list.
- BRD-only: List/Kanban/Calendar view toggle (line 2542-2545) not evident in this mockup (single list view only).
- Open questions: none.

### 36-technician-assignment-dispatch
- BRD: §74 Work Order Detail Screen, `Assign` button (line 2574-2593) | Work item: BOS-006 | Tasks: none yet
- Shows: assignment channel (in-house staff vs. empanelled vendor), technician roster with ratings/specialties, arrival window/duration, and a billing classification step (society-common vs. private-chargeable) with a cost estimate.
- Aligned: maps to the `Assign` button/action on Work Order Detail (line 2586); contractor concept aligns with §75/§76 Contractor screens (line 2596-2627).
- UI-only (not in BRD — do not implement without approval): technician star ratings/reviews, "in-house staff vs. vendor" assignment channel split, society-common-vs-chargeable billing classification at assignment time, preliminary cost-estimate range shown to resident.
- BRD-only: BRD work order fields have `contractor_id` (single reference) and `estimated_cost` (line 1525-1526) — no BRD concept of in-house staff roster or per-technician skill/rating metadata.
- Open questions: does BRD's contractor/work model support "in-house staff" as an assignee distinct from `contractor_id`, or is this new scope requiring a CR?

### 37-job-progress-execution
- BRD: §74 Work Order Detail Screen — status timeline, `Start Work`/`Mark Blocked`/`Add Expense` (line 2574-2593) | Work item: BOS-006 | Tasks: none yet
- Shows: live status timeline (logged → assigned → arrived → in-progress → signoff-pending), elapsed/SLA timers, itemized parts/labor billing log with SKUs.
- Aligned: status timeline concept matches "Status timeline" (line 2577); "Add Expense" (line 2590) loosely covers the parts/labor logging, though BRD models cost as a single `actual_cost_reference` (line 1527), not itemized SKU lines.
- UI-only (not in BRD — do not implement without approval): entry OTP verification for technician site access ("Entry OTP #7492 Verified"), SKU-level parts catalog/pricing, elapsed-time/SLA countdown widgets on the execution screen itself.
- BRD-only: `Mark Blocked` button (line 2588) and generic `Comments`/`Attachments` (line 2582-2583) not shown in this specific screen (may exist elsewhere in the flow).
- Open questions: OTP-gated site access is a physical-security feature with no BRD basis — confirm whether in scope or a UI-only aspiration.

### 38-work-order-completion-signoff
- BRD: §74 Work Order Detail, `Complete` button (line 2589); billing overlaps §25 Maintenance Fee Rules and §26 Expense Rules (line 1439-1500) | Work item: BOS-006 | Tasks: none yet
- Shows: completion summary, before/after photos, itemized settlement ledger (labor + parts + "society admin fee"), payment method selection including bKash/Nagad "Pay Now", resident star-rating + digital signature.
- Aligned: `Complete` action and `actual_cost_reference`/cost concepts (line 1527) are broadly consistent; "Add to Monthly Maintenance Bill" aligns with billing an item to the maintenance invoice cycle (§25).
- UI-only (not in BRD — do not implement without approval; **flag: contradicts explicit exclusion**): in-app "Pay Now via bKash/Nagad Instant MFS" button. BRD explicitly places "Integrated payment gateway" and "bKash/Nagad merchant APIs" under §3.2 Future/Optional Scope (line 93-94) and "Direct payment gateway" under §125 MVP Exclusions (line 3848). Also UI-only: geo-tagged before/after photo capture pairing, resident star-rating + feedback memo, digital-signature/fingerprint signoff, "30-Day Service Guarantee" badge, "Society Administrative Fee (10%)" line item.
- BRD-only: none noted beyond the general Work Order Detail fields.
- Open questions: is the bKash/Nagad "Pay Now" button meant as a future-scope placeholder (disabled) or intended for near-term build? Needs explicit approval given the direct MVP-exclusion conflict.

### 39-notice-board-circulars
- BRD: §77 Announcement List Screen (line 2630-2648); §150.2 Notice Board (line 5383-5406) | Work item: BOS-007 | Tasks: none yet
- Shows: notice feed with Emergency/Pinned circulars, PDF attachments, read/acknowledgment %, an AGM agenda card with RSVP/quorum tracking.
- Aligned: Pinned (line 5390, "Pinned" badge), Read % (line 2642, "28 of 36 Flats Acknowledged (78%)"), priority/Emergency handling (line 5391, "Emergency" tag), attachments and `+ New Announcement`/`+ Post Notice` (line 2645).
- UI-only (not in BRD — do not implement without approval): AGM agenda card with live "Digital Quorum: 22/36 Confirmed" and RSVP action (blends Meetings §29 and unmodeled Polls, see 41 below); a notice attributed to an external government body ("Dhaka North City Corporation"); bottom-nav "Passes" tab (visitor gate pass — future scope, see 43).
- BRD-only: Active/Scheduled/Expired tabs (line 2632-2635) not visibly present as tabs in this mockup (appears to be a single merged feed).
- Open questions: `category` field for notices is explicitly OPEN in BRD (line 5404, "not yet decided") — this mockup implies categories (Emergency, official circular, resolution) that would need that decision resolved first.

### 40-notice-detail-acknowledgment
- BRD: §79 Announcement Detail Screen (line 2673-2687); §150.2 (line 5383-5406) | Work item: BOS-007 | Tasks: none yet
- Shows: circular detail with issuer, priority, validity window, target audience, attachments, and an "Acknowledge Receipt" action.
- Aligned: Content/Attachments/Audience/Published-by/Read-ack-state (line 2675-2680) and `Acknowledge` button (line 2683) match closely; "Valid Till" maps to `expires_at` (line 1563); "requires_acknowledgement" (line 1564) matches the mandatory-ack flow.
- UI-only (not in BRD — do not implement without approval): "Recording Ledger Hash..." / tamper-evident hash framing for the acknowledgment (BRD's audit model, §90/§114, is a standard audit log, not a hash-chained ledger); "Executive Authorization" seal/signature graphic; per-flat "Resident Unit Status" occupant-count card; "Add to Calendar / Set Reminder" action (plausible tie-in to §88 Calendar but not specified here).
- BRD-only: `Edit` (if allowed) and `Expire Now` buttons (line 2684-2685) not shown on this screen.
- Open questions: none beyond the ledger-hash framing above, which should not be implemented as literal blockchain/hash-chain without a decision.

### 41-society-polls-agm-voting
- BRD: **NOT IN BRD**. Grep for "poll", "vote", "voting" across the BRD returns no matches except an unrelated use of "polling" for WebSocket fallback (§150.5, line 5517, "periodic history polling"). | Work item: none (needs CR) | Tasks: none yet
- Shows: AGM resolution voting — weighted votes per deed/unit, quorum tracking (66.7%, "Met §18 Bylaws" — a fictional bylaws reference, not a BRD section), live vote-breakdown by option, historical ballot archive.
- Aligned: n/a — no corresponding BRD capability.
- UI-only (not in BRD — do not implement without approval): the entire polls/voting feature, including per-deed vote weighting, quorum/threshold logic, and a "Live Voting Breakdown". §29 Meeting Management (line 1573-1600) covers meetings/decisions/minutes but has no voting/ballot mechanism.
- BRD-only: n/a.
- Open questions: this is a materially new capability (governance voting) not referenced anywhere in scope (§3.1/§3.2), MVP (§124), or exclusions (§125). Needs a CR and product decision before any implementation.

### 42-resident-directory-intercom
- BRD: §87 Contacts Screen (line 2828-2843) | Work item: none (needs CR — §87 is not referenced by any backlog item, BOS-006 through BOS-009) | Tasks: none yet
- Shows: categorized directory (security/gate staff, facilities, fire station, residents by floor) with call/chat actions, vehicle plate numbers, partially masked phone numbers.
- Aligned: Categories concept (Committee/Owners/Tenants/Contractors/Emergency, line 2830-2835) and `Call`/`Message`/`View Profile` actions (line 2837-2840) map to "Emergency" (fire station), residents, and facilities staff entries here. "Respect data-visibility policy" (line 2842) is broadly consistent with the masked phone number display shown.
- UI-only (not in BRD — do not implement without approval): vehicle license-plate numbers per resident, in-app "intercom" call framing to gate/security posts, per-unit member-count badges.
- BRD-only: none noted.
- Open questions: §87 Contacts is not mapped to any current work item (BOS-001–BOS-009 backlog references list §s 6-18,93-103,123 / 4-5,19,39-42,48-54,138,143 / 20-21,55-62,112 / 22-26,63-71,108,113,143 / 27,30,72-76,83-84 / 8.6-8.7,28-29,77-82,85,115,150 / 43-47,86,111,116,132-134 / 93-99,102,135-136,142,146 — none include §87). This screen needs a work item created before implementation.

### 43-visitor-gate-pass-management
- BRD: FUTURE SCOPE §3.2 (line 96-97, "Visitor management", "Gate access"); also §125 MVP Exclusions (line 3849, "Visitor access") | Work item: none (needs CR) | Tasks: none yet
- Shows: digital guest pass issuance with gate entry code, expected-arrival window, guest parking allocation, delivery-rider gate log with OTP-gated entry approval, "frequent pass" presets.
- Aligned: n/a — BRD explicitly defers this whole capability.
- UI-only (not in BRD — do not implement without approval): entire visitor/gate-pass feature — digital pass generation, delivery-rider OTP entry approval, WhatsApp/SMS pass sharing, guest parking assignment.
- BRD-only: n/a.
- Open questions: none — BRD is explicit and consistent (§3.2 and §125 agree) that this is out of scope for the current build; do not implement without a scope-change decision.

---

## Screens in BRD §72-90 with no mockup in this batch
- §75-76 Contractor List / Contractor Detail
- §78 Create Announcement
- §80-82 Meetings List / Create Meeting / Meeting Detail
- §83-84 Asset List / Asset Detail
- §85 Notifications Screen
- §86 Reports Screen
- §87 Contacts Screen (partially resembled by 42-resident-directory-intercom, but not a direct implementation of §87)
- §88 Calendar Screen
- §89 Settings Screen
- §90 Audit Log Screen
- §150.3 Community Chat (chat channels, messaging, moderation — no mockup screens for chat itself)
