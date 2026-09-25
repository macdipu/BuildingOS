# BOS-011 — Technical spec

Architecture: follows `docs/ARCHITECTURE.md` (Flutter feature-first Clean Architecture; backend
one use case per action). No new service, no DB migration, no new Kafka topic.

## 1. Theme (UI-T01)
- `user_app/lib/app/theme/color_schemes.dart`: brand + Material 3 roles from DESIGN.md `colors`
  (surface/primary/secondary/tertiary/error/outline families map 1:1 to `ColorScheme` fields).
- `text_theme.dart`: DESIGN.md `typography` → `TextTheme` (display/headline = Plus Jakarta Sans,
  body/label = Inter via existing `google_fonts` dep); currency styles as a `ThemeExtension`.
- `app_dimensions.dart`: `rounded` + `spacing` tokens; `app_theme.dart` component themes use them.
- Every existing screen restyles through the theme; no per-screen hard-coded colors added.
- Dark: pending Q-03.

## 2. Splash (UI-T02) — BRD §39
New `features/splash` (presentation + use case `ResolveStartDestination`): read stored token →
if absent `/login`; else load current user + memberships (existing auth/units_ownership repos) →
exactly one building and no pending invitation/application → shell dashboard; otherwise
`/my-buildings`. `AppPages.initial` becomes the splash route. Mockup `mobile/01`.

## 3. Per-role navigation (UI-T03) — BRD §37, D-02
`app/shell`: `NavSet` config per role (Admin/Manager, Owner, Committee, Tenant) exactly as §37;
active set resolved from the active building's `roles` (`property_models.dart`) using Q-01
precedence. Tabs route to existing pages (Units → `building_units_page`, Properties → My
Buildings) or Q-02 behavior. Visual style from mockup `mobile/04+` bottom bar.

## 4. Unit list filter/search/sort (UI-T06 BE, UI-T07 Mobile) — BRD §48
- BE: `GET /api/v1/buildings/{id}/units` add optional `ownerUserId`, `q` (unit-number
  contains, case-insensitive), `sort` (`unitNumber|floor|type`, `,asc|desc`); extend
  `ListUnitsQuery`/`ListUnitsService` + repository; OpenAPI contract updated; existing params and
  defaults unchanged (backward compatible).
- Mobile: filter sheet (Type, Floor, Owner), search field, sort menu on
  `building_units_page.dart`; layout per mockup `mobile/10`.

## 5. Unit form (UI-T08) — BRD §49
`Save & Add Another`: on success keep floor/type/rate, clear unit number/area/bedrooms/notes,
stay on form. Layout per mockup `mobile/11` single page (not the wizard, C-10).

## 6. Restyles (UI-T04, UI-T05, UI-T09)
Layout-only changes to login/OTP, My Buildings (+ §42 subscription warning if missing),
unit detail / ownership / transfer / members. No behavior or API change beyond listed.

## Risks
- Widget/golden tests asserting colors or text styles will need updating (UI-T01).
- Nav change alters entry route; integration tests starting at `/login` need the splash hop.

## Verification
`flutter analyze`, `flutter test` (incl. `test/architecture` layer rules), `mvn -pl
building-service verify` for UI-T06, device preview per screen against `screen.png` (UI-T10).

## 7. Defect fix DEF-01 (QA, 2026-09-25)
Chrome QA: app stuck on splash. `SplashBinding` used `Get.lazyPut` and `SplashScreen` never reads
`controller`, so `SplashController.onReady` never ran. Fix: `Get.put` in the binding (created when the
route opens). Test: pump the real route with `SplashBinding`-style lazy registration so an unread
controller fails the test. Task UI-T11.
