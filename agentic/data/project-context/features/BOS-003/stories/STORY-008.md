# STORY-008: Owner read access and Unit Detail rental tabs

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
A unit owner can see the tenant, lease and rent invoices of units they currently own, and
the Unit Detail screen shows Occupied/Vacant and its Tenant / Lease / Rent tabs, without
exposing other owners' rental details.

## Scope
- Owner-scoped reads in rental-service for tenants, leases and rent invoices of owned units
  (§138 OWNER `tenant.read`, `lease.read`, `rent.read`: owned units).
- Ownership resolution from building-service (HTTP lookup or projection from
  `ownership.transferred`, Q-BOS-003-08); consumer idempotent if events are used (§144).
- Flutter: Unit Detail (mobile/12) header Occupied/Vacant and Tenant, Lease, Rent tabs plus
  `Add Tenant` / `Create Lease` / `Mark Vacant` buttons for write roles (§50); owner
  Properties flow links to these tabs (BOS-011 owner nav set).

## Acceptance Criteria
1. An owner sees tenant/lease/invoices only for units where they hold active ownership; for
   any other unit in the same building they get `ACCESS_DENIED`/404 (§138, §143 Scenario A
   "Owner cannot see unrelated owners' private rental details").
2. Owners never see NID (masked or otherwise) unless Q-BOS-003-09 grants it, nor tenant
   contact fields beyond what Q-BOS-003-09 allows (§94 "Restrict role access").
3. After an ownership transfer, the new owner gains and the former owner loses current-unit
   rental access from the transfer's effective time (§127 "New owner receives access
   according to effective date"; UO-D02 immediate effect); former-owner historical access
   follows the F4 history-visibility policy (units-ownership DECISIONS).
4. An ownership transfer does not modify, end or re-assign the unit's lease (§127, §143
   Scenario E).
5. If ownership events are consumed, a redelivered `ownership.transferred` (same eventId)
   changes nothing (§144 idempotent consumers).
6. Unit Detail shows Occupied when the unit has an `ACTIVE`/`NOTICE_GIVEN` primary lease,
   otherwise Vacant (§50, §20). `Mark Vacant` maps to the STORY-004 move-out path, not a
   separate state (Q-BOS-003-18).
7. Owners have read-only access: no create/edit/activate/terminate controls are rendered and
   the backend rejects such calls (§138; Flutter does not decide authorization, §144).

## Tasks
Proposed task lanes:
- BE: owner authorization policy + ownership lookup/projection, owner-scoped queries — M
- Mobile: Unit Detail Tenant/Lease/Rent tabs and header state, owner read-only views — M
- QA: owner vs unrelated owner vs former owner matrix, transfer-effective-time test — S

Size: **M** (Rent tab needs STORY-005; Tenant/Lease tabs can ship after STORY-003).

## Dependencies
- STORY-003 (Tenant/Lease tabs), STORY-005 (Rent tab).
- BOS-010 F4 ownership and `ownership.transferred` contract (delivered).
- Questions: Q-BOS-003-08, -09, -18.

## Out of Scope
- Owner dashboard / My Properties rent figures (§44, §111 projections) → BOS-008.
- Maintenance and Documents tabs content beyond the lease agreement (BOS-004, document scope).

## References
BRD §20, §44, §50, §94, §111, §127, §138, §143 A/E, §144; units-ownership DECISIONS
(UO-D01, UO-D02, history visibility); contracts/kafka/ownership-transferred.v1.schema.json;
gaps/mobile-core.md 12.
