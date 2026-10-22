# STORY-003: Payment detail, payment history and PDF receipt

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-004 ([EPIC.md](../EPIC.md))

## Objective
Collectors and payers can open any payment they are allowed to see, view its allocations and audit
reference, and download/share a PDF receipt that contains every §132 field.

## Scope
- Queries: `GetPaymentQuery`, `ListPaymentsQuery` (by unit, payer, status, date), `RenderReceiptPdfQuery`.
- API: `GET /api/v1/payments`, `GET /api/v1/payments/{id}`, `GET /api/v1/payments/{id}/receipt.pdf`.
- PDF rendering inside finance-service from immutable receipt + payment + allocation data (Q-BOS-004-28).
- Flutter: Payment Detail screen (§64, mobile/26): Receipt number, Payer, Amount, Method, Time, Collector,
  Invoice allocations, Audit reference; buttons `Download Receipt`, `Share Receipt`, `Reverse Payment`
  (visible only with permission; action wired in STORY-004); no delete. Payment list under Finance tab and
  owner "Payments" tab (C-1 nav).

## Acceptance Criteria
1. Payment detail returns all §64 display fields, including the full invoice allocation list (gap file:
   mockup shows only one line — BRD wins).
2. The PDF contains: building name, receipt no, tenant/owner, unit, payment category, amount, payment
   method, paid date, allocation breakdown, collector, generated timestamp (§132).
3. The receipt number shown is the server-generated §108 number from STORY-002 — never a client-made
   value (C-7).
4. Re-downloading the same receipt yields the same receipt number and amounts (financial data immutable,
   §113.6); a reversed payment's receipt/detail is clearly marked REVERSED (§24).
5. No API or UI offers delete of a payment (§64 "No delete", §113.7).
6. Authorization: finance roles see all payments of the active building; an owner sees only payments for
   units they own or paid themselves (§4 OWNER "View rent payments", "View maintenance dues"; §143 A
   "Owner cannot see unrelated owners' private rental details"; Q-BOS-004-29). Cross-building access → 404.
7. `Reverse Payment` is shown only when the caller has the reversal permission (§64), and the backend
   still enforces it (§93).
8. Dates/times shown in Asia/Dhaka, amounts in BDT (§109); PDF labels come from localization keys, no
   single language hard-coded into domain records (§109).

## Tasks
Proposed lanes only — no TASK files yet.
- BE: detail/list queries, PDF renderer, OpenAPI — M
- Mobile: payment detail (mobile/26), payment list, download/share via OS share sheet — M
- QA: §132 field checklist, owner isolation, immutability — S
- Size: **L** (PDF library choice needs technical approval; uncertainty medium).

## Dependencies
STORY-002. Q-BOS-004-28, -29.

## Out of Scope
QR / hash verification and "Net Landlord Credit" (mobile/26 UI-only, C-4); push/SMS receipt delivery
(BOS-007); tenant view (requires BOS-003 tenant identity — added with STORY-008).

## References
BRD §4, §24, §64, §108, §109, §113, §132, §143 A; UI-INDEX mobile/26, C-4, C-7;
gaps/mobile-rental-finance.md §26.
