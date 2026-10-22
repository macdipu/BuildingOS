# STORY-003: Manager dashboard

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Property/rent managers see today's collection work: due today, overdue, today's collections, the
collection queue, overdue tenants, lease expiry and vacant units (§45).

## Upstream event sources
- rental-service (BOS-003): `tenant.created`, `lease.activated`, `lease.terminated`, `unit.vacated`,
  `rent.invoice.created`, `rent.invoice.overdue`, `tenant.notice.submitted`
- finance-service (BOS-004): `payment.recorded`, `payment.allocated`, `payment.reversed`
- building-service: `unit.created`, `unit.updated` (vacant units baseline)

## Scope
- Projections `rent_due_view`, `lease_expiry_view`, vacant-unit status in `unit_occupancy_view`.
- Manager shape of the dashboard API.
- Flutter manager dashboard (mockup `mobile/07-manager-dashboard`): cards Due today, Overdue, Today's
  collections; sections Collection queue, Overdue tenants, Lease expiry, Vacant units; buttons Collect
  Rent, Add Tenant, Create Lease, Record Move-Out (§45) navigating to BOS-003/004 screens.

## Acceptance Criteria
1. "Due today" / "Today's collections" use the Asia/Dhaka calendar day (§109).
2. "Overdue" counts invoices reported overdue by `rent.invoice.overdue` and still outstanding after allocations (§8.3, §112 overdue detection).
3. A partial payment moves the invoice to partially-paid with the remaining due in the queue (§143 Scenario B).
4. Lease expiry section lists leases by end date; window per Q-BOS008-09 (BRD §115 reminder windows 60/30/7 days are the recommended default).
5. Buttons render only when the caller holds the matching permission (`payment.create`, `tenant.create`,
   `lease.create`, move-out) (§45, §137–138); hidden buttons are also rejected server-side by their owning service.
6. Caller without manager-type role in the building gets 403 for the manager shape (§110).
7. Mockup-only bKash TrxID verification, WhatsApp reminder and inspection checklist are not built (gaps/mobile-core.md 07).
8. Idempotent consumers; replay does not change counts (§16).

## Tasks
Proposed lanes:
- BE/DB: rent-due, lease-expiry, vacancy projections and manager query — M
- Mobile: manager dashboard screen and navigation to rental/finance actions — M
- QA: day-boundary (Asia/Dhaka), partial-payment, overdue, permission-hidden buttons — S
Size: **M** (uncertainty: medium — overlaps STORY-002 handlers).

## Dependencies
STORY-001; BOS-003; BOS-004 (payment events; Collect Rent screen). Q-BOS008-06, Q-BOS008-09.

## Out of Scope
Collect Rent / Add Tenant / Create Lease / Move-Out flows themselves (BOS-003/004).

## References
BRD §45, §109, §112, §115, §137–138, §143 B; UI `mobile/07-manager-dashboard`; gaps/mobile-core.md §07.
