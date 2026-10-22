# STORY-002: Owner dashboard and cross-building portfolio projection

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Owners see per-building and cross-building rent/occupancy/maintenance figures for the units they own,
built from projections (§44, §111 `owner_dashboard_view` and `owner_portfolio_view`, §143 Scenario A).

## Upstream event sources
- building-service (BOS-010/STORY-001): `unit.created`, `unit.updated`, `ownership.assigned`, `ownership.transferred`
- rental-service (BOS-003): `lease.activated`, `lease.terminated`, `unit.vacated`, `rent.invoice.created`, `rent.invoice.overdue`
- finance-service (BOS-004): `payment.allocated`, `payment.reversed`, `maintenance.invoice.created`

## Scope
- Projections `owner_dashboard_view` and `owner_portfolio_view` with exactly the §111 fields.
- Dashboard API owner shape; `GET /api/v1/reports/portfolio` for the cross-building view (§149.14).
- Flutter owner dashboard (mockup `mobile/06-owner-dashboard`): cards Total units owned, Monthly
  expected rent, Rent collected, Outstanding rent, Maintenance due, Occupancy; sections Unit performance,
  Recent payment, Tenant alerts, Lease expiry; buttons View Properties, View Rent Ledger, View Maintenance,
  Download Statement (§44). Download Statement is wired in STORY-008.

## Acceptance Criteria
1. An owner of 3 flats sees 3 units, occupancy per unit, rent expected, rent received, outstanding and
   maintenance due (§143 Scenario A).
2. The owner cannot see unrelated owners' private rental details; portfolio rows exist only for units with
   active ownership (§143 A, §111, §138 `report.read: owner reports`).
3. Outstanding rent is computed from invoice allocation events (`payment.allocated`, `payment.reversed`),
   never from a raw payment sum (§144 "Calculate outstanding purely from payment sum without allocation").
4. A reversed payment restores the outstanding amount in the projection (§24, §8.4 `payment.reversed`).
5. After `ownership.transferred`, the old owner's projection rows for that unit stop at the effective date
   and the new owner's rows begin; history is preserved (§127, §143 E).
6. Co-owned units show the owner's `ownership_share` (§111 portfolio field, §149.14 "ownership percentage").
7. Tenant summary and rent summary on portfolio rows show only "when permitted" (§149.14) — permission source per Q-BOS008-04.
8. Opening a property row deep-links into that building's authorized context (§149.14).
9. Mockup-only "Recent Payouts" 3% management-fee card is not built (gaps/mobile-core.md 06; C-4).
10. Duplicate/replayed events do not double-count collected rent (§16).

## Tasks
Proposed lanes:
- BE/DB: owner projections + handlers for 11 event types, owner dashboard/portfolio queries — L
- Mobile: owner dashboard screen, portfolio list, deep link — M
- QA: Scenario A and E fixtures, reversal case, isolation between two owners in one building — M
Size: **L** (uncertainty: high — depends on BOS-003/004 event payload shapes).

## Dependencies
STORY-001; BOS-003 (rental events); BOS-004 (finance events). Q-BOS008-03 (entitlement gating),
Q-BOS008-08 (relationship to existing BOS-010 `My Properties`).

## Out of Scope
Owner statement export (STORY-008); payout/management-fee math; editing any data.

## References
BRD §44, §111, §127, §138, §143 A/E, §144, §149.14; UI `mobile/06-owner-dashboard`; gaps/mobile-core.md §06.
