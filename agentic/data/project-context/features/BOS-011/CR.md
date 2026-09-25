# BOS-011 — CR: align delivered mobile screens to Stitch UI

Type: change_request (brownfield, module `user_app` + one `building-service` query extension)
Status: IMPLEMENTATION (technical approved by macdipu 2026-09-25)
Raised: 2026-09-25 by operator (macdipu), chat: "yes use stitch blue, per-role nav, create the CR"

## Source
- UI mockups + gap analysis: [`../../ui/stitch/UI-INDEX.md`](../../ui/stitch/UI-INDEX.md)
- BRD §37 (navigation), §39–42 (splash/login/OTP/My Buildings), §48–54 (units/ownership)
- Delivered baseline: BOS-010 TASK-002/005/006/007 (auth), F4-T2..T8 (units/ownership); all
  COMPLETED and released. They are **not reopened** — this CR layers changes on top.

## Delta
1. Visual: replace current brand theme (navy `#0A1A2F` / yellow / orange) with the Stitch
   "Proptech Enterprise" tokens (`ui/stitch/design-system/DESIGN.md`) — decision D-01.
2. Navigation: replace the starter `AppShell` (Home/Explore/Account) with BRD §37 per-role
   bottom navigation — decision D-02.
3. BRD gaps found in delivered screens:
   - §39 Splash never built (app starts at `/login`).
   - §48 Unit list lacks Owner filter, Search, Sort (Type/Floor filters exist in API only).
   - §49 Unit form lacks `Save & Add Another`.
   - §42 subscription/access warning not confirmed on My Buildings.
4. Restyle delivered screens to their mockup layout: login (02), OTP (03), My Buildings (04),
   unit list (10), unit form (11), unit detail (12), ownership transfer (14). Building-application
   and members screens have no mockup — theme pass only.

## Out of scope
- Mockup-only elements flagged in UI-INDEX conflicts: C-10 (unit wizard, parking/utility meters,
  transfer OTP gate / legal-instrument type), C-11 (Google sign-in button), everything in C-2..C-9,
  C-12, C-13.
- Unit list columns Tenant / Occupancy / Rent / Maintenance due and filters Occupancy / Due status
  (need BOS-003/004 data) — shown only when those modules ship.
- Dashboards content (BOS-008), finance/work/community screens (BOS-003/004/006/007): nav tabs
  point at them but their screens are not built here.
- Back-office web (BOS-010 F6-T6..T9 plan from the mockups directly).

## Artifacts
[DECISIONS.md](DECISIONS.md) · [TECH-SPEC.md](TECH-SPEC.md) · [TASKS.md](TASKS.md) · `tasks/UI-T*.md`
