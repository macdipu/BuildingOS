# BOS-011 QA (UI-T10)

Date: 2026-09-25. Device: Android emulator `Pixel_4a` (API 37, debug, `flutter run -d emulator-5554`,
`API_BASE_URL=http://10.0.2.2:8080/`). Backend: local stack (`infra/local/compose.yaml`) with
building-service rebuilt from `main` (includes UI-T06). Data: `seed-local-qa.sql` (local DB only):
user `01711111112` = BUILDING_ADMIN + OWNER of one ACTIVE building, 2 floors, 3 units, owns 4A.
Login with development OTP `000000` (BOS-010 D-01).

Round 1 (Chrome web build): FAIL — DEF-01, app stuck on splash (see `../qa-result-1.json`).
Fixed in UI-T11 `d8e851f`. Round 2 below on the emulator (operator asked to run on emulator).

| # | Screen | Mockup | Result |
|---|---|---|---|
| 01 | Splash → Login (no session) | mobile/01, 02 | PASS — splash routed to login; login per mockup 02, no Google button |
| 02 | OTP | mobile/03 | PASS — Verify Identity, sent-to number, Change Number, resend timer, Verify & Continue |
| 03 | After OTP → Dashboard | §39, §37 | PASS — single building → shell; admin+owner → manager nav (Q-01); dashboard placeholder (BOS-008) |
| 04 | Units tab | mobile/10 | PASS — search, filter, sort, + Add unit, floors, unit rows |
| 05 | Search `G` | §48 | PASS — live API `q` returns G1 only |
| 06 | Unit detail G1 | mobile/12 | PASS — header card + specifications, Ownership / Edit unit |
| 07 | Add unit | mobile/11, §49 | PASS — single page, Save, Save & Add Another, Cancel |
| 08 | More tab | §37 | PASS — Switch building, building applications |
| 09 | My Buildings | mobile/04, §42 | PASS — status pill, roles, owned units, Open Building, applications entry, My Properties |
| 10 | Cold restart with session | §39 | PASS — splash → Dashboard, no login |

Covered by widget tests, not re-exercised on device: owner-only / tenant nav sets, coming-soon tabs,
suspended-building warning, filter sheet, transfer steps, Save & Add Another flow.

Regression: `flutter analyze` clean; `flutter test` 94 passed, 1 skipped; building-service
`mvn verify` 118/0 (UI-T06).

Visual deviations (not defects, follow-up candidates):
- Login/OTP CTA (`CommonButton`) uses primary `#004AC6`; mockups use primary-container `#2563EB`.
- `PropertyAction` buttons (Save, Edit unit) are compact/centered; mockups use full-width CTAs.
- Mockup-only content intentionally absent per UI-INDEX conflicts (C-10, C-11) and BOS-003/004 data.
