# STORY-006: Rent overview and invoice detail

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
An operator can see the month's expected and outstanding rent for the building, filter
invoices, open an invoice, and trigger generation, from the app.

## Scope
- Rental-owned read APIs: invoice list (filters month, owner, unit, tenant, status tab;
  §61) and detail (§62), and a monthly summary query for the §61 cards that rental-service
  can answer from its own data.
- Flutter: Rent Overview (mobile/23) and Rent Invoice Detail (mobile/24); Tenant Detail Rent
  tab (§57); Lease Detail Invoice history section (§60).

## Acceptance Criteria
1. Rent Overview cards: "Expected this month" and "Outstanding" come from rental invoices
   (§61, §21); "Collected" and "Advance balance total" depend on BOS-004 and are shown as
   unavailable (not zero, not fabricated) until then (BOS-011 Q-02 placeholder pattern).
2. Tabs Due / Paid / Partial / Overdue and filters Month / Owner / Unit / Tenant (§61) work;
   tabs with no possible data yet (Paid/Partial before BOS-004) are present but empty.
   The mockup's "All" tab does not replace "Due" (gap file 23).
3. Buttons: `Generate Invoices` calls STORY-005's manual action with confirmation; `Collect
   Rent` is hidden/disabled until BOS-004; `Export` → BOS-008 (Q-BOS-003-28).
4. Invoice Detail shows invoice no, billing period, tenant, unit, charges, discounts, paid,
   outstanding, allocations (§62); paid/allocations show none until BOS-004.
5. No platform-fee / landlord-payout split is shown anywhere (C-4); no escrow sync or
   "Verification Pending (bKash)" rows (gap file 23, UI-INDEX rule 2).
6. `Download Statement` and `Send Reminder` (§62) are hidden until their owners exist
   (statement → BOS-008, reminder → BOS-007) — Q-BOS-003-28.
7. Filtering by Owner uses current ownership from building-service and never exposes other
   owners' private rental details to an owner caller (§143 Scenario A; full owner view is
   STORY-008).
8. Lists are paginated (§118); amounts formatted in BDT with localization keys (§109).
9. Only roles with `rent.read` for the building see this screen (§137, §138 MANAGER).

## Tasks
Proposed task lanes:
- BE: invoice list/detail queries, monthly summary query, owner filter via ownership lookup — M
- Mobile: Rent Overview, Invoice Detail, Rent tab, Invoice history section — M
- QA: filter correctness, cross-building isolation, placeholder states — S

Size: **M**.

## Dependencies
- STORY-005.
- Questions: Q-BOS-003-02, -08, -28.

## Out of Scope
- Reporting projections (§111) and dashboards → BOS-008.
- Collect Rent / Record Payment (§63) → BOS-004.

## References
BRD §21, §57, §60, §61, §62, §109, §111, §118, §137, §138, §143 A; UI-INDEX C-4;
gaps/mobile-rental-finance.md 23, 24; BOS-011 Q-02.
