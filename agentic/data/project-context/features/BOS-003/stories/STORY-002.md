# STORY-002: Lease drafts and lease list

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-003 ([EPIC.md](../EPIC.md))

## Objective
An operator can create, edit and list lease drafts linking a unit and a tenant, with the
agreed rent terms and an optional agreement document, so leases can be prepared before
activation.

## Scope
- `lease` feature in rental-service: `CreateLeaseUseCase` (§12, `CreateLeaseCommand` §13),
  update draft, get, list.
- Fields per §59: unit, tenant, start date, end date, monthly rent, security deposit,
  advance rent, due day, late fee policy, parking charge, other recurring charges, agreement
  document.
- `Lease` + `LeaseTenant` (multiple occupants, §20; shape per Q-BOS-003-12); state `DRAFT`.
- Unit validation against building-service (unit exists in the active building, eligible
  type) — Q-BOS-003-08, Q-BOS-003-13.
- Agreement document upload/download behind a storage port (§20 "Lease files are documents,
  not blobs stored inside the Rental DB"; D-28 pattern) — Q-BOS-003-07.
- Event `lease.created` v1 via outbox (§8.3).
- Gateway route `/api/v1/leases/**` (§105).
- Flutter: Lease List (mobile/20), Create Lease (mobile/21) with `Save Draft` / `Cancel`;
  Tenant Detail Lease tab and `Create Lease` button (§57); Add Tenant `Save & Create Lease`
  wired (§56).

## Acceptance Criteria
1. `POST /api/v1/leases` creates a `DRAFT` lease for a unit and tenant of the same active
   building; a unit or tenant of another building is rejected (`RESOURCE_NOT_FOUND`/
   `ACCESS_DENIED`, tenant isolation).
2. Required fields and value checks: monthly rent > 0 with currency BDT (§109, §113 #9);
   deposit, advance rent, parking and other recurring charges ≥ 0 when supplied; end date
   after start date; due day within the rule set by Q-BOS-003-14/22. Invalid input returns
   `VALIDATION_ERROR` with field details.
3. Security deposit and advance rent are stored as separate lease terms, not as rent (§20
   "Security deposit is recorded separately from rent"); no payment or ledger record is
   created (BOS-004).
4. Only a `DRAFT` lease can be edited (`Edit Draft`, §60); edits to non-draft leases are
   rejected; concurrent edits return `VERSION_CONFLICT` (§129).
5. Agreement document: upload/download goes to object storage through a port; only metadata
   (key, name, type, size, uploader, time) is stored in rental_db (§20); access is limited to
   the same roles that can read the lease; file type/size bounds are technical safeguards.
6. Lease List shows Unit, Tenant, Start, End, Rent, Status (§58 Row) with filters Active /
   Expiring / Notice / Expired / Unit and search (§58); the mockup's "Terminated" tab is added
   alongside "Expired" since both are distinct §20 states (gap file 20).
7. Creating a draft writes an audit row and `lease.created` in the same transaction (§15).
8. Writes are allowed only for the write roles (Q-BOS-003-04) and building states
   (Q-BOS-003-11).
9. Create Lease shows only §59 fields; mockup-only covenants, lock-in period, annual
   escalation and stamp-paper clauses are not shown (UI-INDEX rule 2, gap file 21).

## Tasks
Proposed task lanes:
- BE/DB: lease + lease_tenant + lease_document tables, create/update/get/list use cases,
  unit lookup client — M
- BE: document storage port + MinIO adapter, upload/download endpoints — M
- BE: `lease.created` contract + outbox — S
- Mobile: Lease List, Create Lease (draft), Tenant Detail Lease tab, Save & Create Lease
  wiring — M
- QA: cross-building isolation, draft-only edit, document access tests — S

Size: **L** (moderate uncertainty: document storage and occupants model questions).

## Dependencies
- STORY-001.
- Questions: Q-BOS-003-07, -08, -12, -13, -14.

## Out of Scope
- Activation, rent schedule, invoices (STORY-003, STORY-005).
- Collecting deposit / advance rent (BOS-004).
- Lease List `Export` (§58) → BOS-008 (Q-BOS-003-28).
- Lease-card actions Renew / Move-out on the list (handled on Lease Detail, STORY-004).

## References
BRD §8.3, §12, §13, §20, §56, §57, §58, §59, §60, §105, §109, §113, §128, §129; D-28;
gaps/mobile-rental-finance.md 20, 21.
