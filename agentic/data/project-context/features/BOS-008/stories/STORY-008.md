# STORY-008: PDF and Excel export, owner statement, share

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-008 ([EPIC.md](../EPIC.md))

## Objective
Users holding `report.export` export the core reports to PDF and Excel, owners download their statement
from the owner dashboard, and exports can be shared from the device (§86 buttons, §44 Download
Statement, §132–133, §146 criterion 22).

## Upstream event sources
None new — reads STORY-007 report projections and STORY-002 owner projections. Receipt PDF data comes
from finance-service `receipt.generated` (BOS-004) if Q-BOS008-11 places receipt rendering here.

## Scope
- `GET .../{reportType}/export?format=pdf|xlsx` (synchronous, safe row limit — §133).
- Excel: numbers numeric, dates as dates, currency formatting, header row, filterable table, report
  metadata sheet/rows (§133).
- PDF: report rendering with building name, filters, generated timestamp; owner statement PDF.
- Optional: PDF receipt with §132 fields (per Q-BOS008-11).
- Flutter: Export PDF / Export Excel / Share on report view; Download Statement on owner dashboard;
  file saved/shared via platform share sheet.

## Acceptance Criteria
1. Export requires `report.export` in the building (§137); without it the API returns 403 and the buttons are hidden.
2. Excel output satisfies every §133 bullet (verified by parsing the file in tests).
3. Exports above the safe row limit are rejected with a clear error, not truncated silently (§133; limit value Q-BOS008-16).
4. Exported totals equal the on-screen report for the same filters (§116).
5. Amounts in BDT, dates Asia/Dhaka; labels localized en/bn; Bangla text renders correctly in PDF (§109).
6. If a receipt PDF is produced it uses server receipt data and §108 number format, with all §132 fields (QR optional) (§132, C-7).
7. Export events are audited if Q-BOS008-07 decides so (§114 "Report export optionally").
8. Export is excluded from the §134 API P95 target ("excluding file/report generation").

## Tasks
Proposed lanes:
- BE: export use cases, Excel writer, PDF renderer (library choice in technical stage), row limit — L
- Mobile: export/share actions, file handling, owner Download Statement — M
- QA: Excel format assertions, Bangla PDF rendering, permission, row-limit — M
Size: **L** (uncertainty: high — Bangla font rendering in PDF and library selection).

## Dependencies
STORY-007; STORY-002 (owner statement); BOS-004 for receipt data. Q-BOS008-07, -11, -16, Q-BOS008-03 (entitlement).

## Out of Scope
Asynchronous/large exports (§133 "later"); scheduled/email delivery; audit export (Q-BOS008-07).

## References
BRD §44, §86, §108–109, §114, §116, §132–134, §137, §146 (22); UI-INDEX C-7.
