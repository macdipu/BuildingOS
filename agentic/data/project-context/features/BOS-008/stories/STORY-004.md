# STORY-004: Tenant dashboard

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Tenants see what they owe, when it is due, their advance balance, latest receipts, notices and lease
summary on the Home tab (§46).

## Upstream event sources
- rental-service (BOS-003): `lease.activated`, `lease.terminated`, `rent.invoice.created`, `rent.invoice.overdue`
- finance-service (BOS-004): `payment.allocated`, `payment.reversed`, `receipt.generated`,
  `advance.balance.changed`, `maintenance.invoice.created` (only if tenant-visible)
- communication-service (BOS-007): `announcement.published`, `announcement.expired` (targeted notices)

## Scope
- Projection `tenant_summary_view` (per tenant per building: current rent due, next due date, advance
  balance, latest receipts refs, lease summary) and tenant notice list (metadata only).
- Tenant shape of the dashboard API.
- Flutter tenant Home (mockup `mobile/08-tenant-dashboard`): cards Current rent due, Maintenance if
  tenant-visible, Next due date, Advance balance; sections Latest receipts, Notices, Lease summary;
  buttons View Payment History, Download Receipt, View Lease, Contact Manager (§46).

## Acceptance Criteria
1. A tenant sees only their own lease, invoices, payments and receipts (§138 TENANT: `rent.read: own invoices`, `payment.read: own payments`, `lease.read: own active lease`).
2. After paying 50,000 against a 20,000 invoice the tenant sees the invoice paid and advance balance 30,000 (§143 Scenario C).
3. Maintenance card appears only when maintenance is tenant-visible (§46); the visibility rule is Q-BOS008-10.
4. Notices shown are only those targeted to the tenant (§138 `announcement.read: targeted`); EMERGENCY notices are included (§115).
5. Download Receipt opens the server-canonical receipt (§132 "Server receipt data is canonical"; §108 numbering, C-7); rendering per STORY-008 / Q-BOS008-11.
6. Contact Manager uses only permitted contacts (§4 TENANT "View permitted contacts"); no new directory feature (§87 unassigned).
7. Mockup-only "Report Issue" shortcut is not built here (gaps/mobile-core.md 08; BOS-006 owns work requests).
8. Idempotent consumers; reversed payment updates the due amount (§16, §24).

## Tasks
Proposed lanes:
- BE/DB: tenant summary projection, notices metadata projection, tenant query — M
- Mobile: tenant Home screen, links to payment history/lease/receipt — M
- QA: Scenario C, cross-tenant isolation in same unit/building, targeted notices — S
Size: **M** (uncertainty: medium — notice targeting depends on BOS-007 payload).

## Dependencies
STORY-001; BOS-003; BOS-004; BOS-007 (notices — the notices section can ship empty-hidden until BOS-007 lands, per Q-BOS008-12). Q-BOS008-10, Q-BOS008-11.

## Out of Scope
Payment history and lease screens themselves (BOS-003/004); in-app payment (C-9).

## References
BRD §4 (TENANT), §46, §108, §115, §132, §138, §143 C; UI `mobile/08-tenant-dashboard`; gaps/mobile-core.md §08; UI-INDEX C-7, C-9.
