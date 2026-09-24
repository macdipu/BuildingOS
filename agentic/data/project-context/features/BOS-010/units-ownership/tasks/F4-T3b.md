# F4-T3b — XLSX unit sheet import

## Category
DB/Integration (building-service)

## Objective
Accept Excel (.xlsx) unit sheets in the F4-T3a batch preview, as data only.

## Decision
Operator chose a built-in reader over a third-party parser (chat, 2026-09-24:
"Built-in reader"). No new dependency: JDK zip + StAX.

## Scope
- First worksheet only; shared strings, inline strings, numbers and booleans as text.
- Same header columns and row mapping as CSV (shared table mapper).
- Rejected: formula cells, macro-enabled content (`vbaProject.bin`), DTDs/external
  entities, more than 64 zip entries, decompressed size above 16 MiB or 100× the upload,
  and more than 500 data rows. The 1 MiB upload limit from F4-T3a still applies.

## Acceptance Criteria
- A valid .xlsx previews identically to the equivalent CSV.
- Formula, macro, DTD and zip-bomb sheets are rejected without partial rows.
- F4-T3a CSV behaviour and earlier regressions pass.

## References
../TECH-SPEC.md "Bulk validation"; F4-T3a.

## Status
COMPLETED 2026-09-24 (run RUN-C73FA516545143FAA9B35B168EEFE0C9). XlsxUnitSheetParserTest 3 plus the
batch API test's .xlsx case; building-service verify 89/0, ArchUnit 7/0, platform-web 5/0. F4-T3 is complete.
