# STORY-009: Tenant access — invitation, my lease, my invoices, current due

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
A tenant can be invited by phone, sign in, land on the tenant navigation, and see their own
active lease, their rent invoices and their current rent due and next due date — the rental
data the §46 Tenant Dashboard needs.

## Scope
- building-service: add `TENANT` to `BuildingRole` and a tenant invitation reusing the
  existing invite → claim flow (phone-verified claim, F4 membership policy; ADR-F4-001
  membership authority) — Q-BOS-003-05.
- rental-service: link the `Tenant` record to the claimed global user id (§110.1 one global
  identity); self-scoped queries: my tenancy, my active lease, my invoices,
  `GetCurrentRentDueQuery` (§13) returning current rent due and next due date.
- Flutter: tenant nav set becomes reachable (BOS-011 D-02, nav_sets.dart `TENANT` mapping);
  tenant Home shows rental cards Current rent due, Next due date and Lease summary from these
  queries (§46); `View Lease` → read-only Lease Detail; Payments tab lists own invoices
  (§37 Tenant nav, §138 TENANT `rent.read: own invoices`). Layout per mobile/08 (Q-BOS-003-29
  on whether the card UI belongs here or BOS-008).
- Operator UI: `Invite to app` action on Tenant Detail (fits §4 PROPERTY_MANAGER "Tenant
  onboarding"; exact button placement not in §57 → Q-BOS-003-05).

## Acceptance Criteria
1. An operator can invite a tenant by the tenant's phone; claiming requires an authenticated,
   phone-verified account whose phone matches (F4 invitation policy); no duplicate global user
   is created (§110.1, §144 "Duplicate the same human User record per building").
2. After claim the user has a `TENANT` membership in that building and the app shows the
   tenant nav set (Home/Payments/Community/Profile, §37); precedence with other roles follows
   BOS-011 Q-01.
3. A tenant reads only their own tenant profile, own active lease and own invoices (§138
   TENANT); requests for other tenants, units or buildings return `ACCESS_DENIED`/404.
4. Current rent due = sum of outstanding (§21 `invoice total − allocated payment`) on the
   tenant's open invoices; next due date = earliest due date of an open invoice or the next
   scheduled period's due date per the rent schedule (Q-BOS-003-22). Values are BDT/Asia/Dhaka
   (§109).
5. Advance balance and latest receipts (§46) are shown as unavailable until BOS-004; maintenance
   "if tenant-visible" (§46) is not shown (BOS-004).
6. Tenant sees their own NID masked by default (§94); reveal per Q-BOS-003-09.
7. Revoking the tenant's membership (existing revoke flow) removes their access to rental data
   immediately; their tenant/lease history is retained (§20, units-ownership history policy).
8. Whether tenant reads require the tenant's own subscription follows Q-BOS-003-06; the
   recommended default never locks a tenant out of their own dues.
9. Invitation send/claim/revoke are audited in building-service (existing MembershipAudit
   pattern; §114 "Role changes").

## Tasks
Proposed task lanes:
- BE/DB (building-service): TENANT role, tenant invitation kind, migration, audit — M
- BE (rental-service): tenant↔user link, self-scoped queries, current-due query — M
- Mobile: invite action, tenant Home rental cards, My Lease, Payments invoice list — M
- QA: tenant isolation matrix, claim-phone-mismatch test, revoke test, nav precedence test — S

Size: **L** (cross-service; uncertainty on Q-BOS-003-05/06/29).

## Dependencies
- STORY-003 (lease), STORY-005 (invoices/due).
- building-service membership/invitation (BOS-010 F4, delivered).
- Questions: Q-BOS-003-05, -06, -09, -22, -29.

## Out of Scope
- Tenant self-service notice or payment (§63; C-9 no in-app gateway).
- Notices section of §46 → BOS-007; dashboard projections → BOS-008.
- `Contact Manager` (§46) needs §87 contacts/directory — unassigned (BACKLOG note on mobile 42).

## References
BRD §4 TENANT, §13, §21, §37, §46, §94, §109, §110.1, §114, §138, §144; ADR-F4-001;
units-ownership DECISIONS (invitation, history visibility); BOS-011 D-02, Q-01;
gaps/mobile-core.md 08.
