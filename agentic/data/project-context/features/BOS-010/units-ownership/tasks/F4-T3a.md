# F4-T3a — Unit batch generation, CSV import, preview and commit

## Category
DB/Integration (building-service)

## Objective
Bulk unit setup per BRD §149.9 steps 2–3 and UO-04, reusing the F4-T2 unit validator.

## Scope
- `POST /api/v1/buildings/{b}/unit-batches/preview` from (a) JSON rows, (b) a generator
  (existing floors × count with a number pattern, a start index for ranges, or a template
  floor whose layout is duplicated onto target floors), or (c) a CSV upload.
- Preview is advisory: per-row field/code errors, never silently dropped rows.
- `POST /api/v1/buildings/{b}/unit-batches/commit`: reviewed rows + operationId; revalidates
  under the building lock and creates all rows or none. Idempotent replay returns the same
  batch; a changed payload under the same operationId → IDEMPOTENCY_CONFLICT.
- V7: `building_unit_batch` + `building_unit.batch_id`; audit per unit and per batch.

## Implementation notes
Safeguards (technical, not subscription limits): 500 rows per preview/commit, 1 MiB CSV.
CSV is parsed as data: RFC 4180 quoting, UTF-8 (BOM tolerated), required header columns
`number, floor, type, areaSqft`, optional `bedrooms, defaultMaintenanceRate, notes`; a
cell starting with `=` is rejected as a formula. Pattern tokens: `{floor}` (floor display
order), `{n}`, `{nn}` (zero-padded), `{letter}` (A–Z). Rows reference floors by id or by
label; floors are not created implicitly.

## Acceptance Criteria
- A duplicate row (in the batch or against existing units) is reported on that row;
  commit with any invalid row persists nothing.
- Concurrent commits never create duplicate normalized numbers.
- Formula cells, oversized files and too many rows are rejected.
- F2/F4-T1/T2 regressions pass.

## References
../TECH-SPEC.md "Bulk validation"; ../DECISIONS.md "Bulk operations"; UO-03/04.

## Out of Scope
XLSX (F4-T3b, needs a reviewed parser dependency), ownership, gateway, mobile.

## Status
COMPLETED 2026-09-24 (run RUN-C73FA516545143FAA9B35B168EEFE0C9). UnitBatchParsingTest 3 +
UnitBatchApiIntegrationTest 6; building-service verify 86/0, ArchUnit 7/0, platform-web 5/0.
XLSX support added by F4-T3b.
