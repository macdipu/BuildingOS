# UI-F01 — Stitch CTA color and full-width actions

## Category
Mobile

## Objective
Match Stitch primary CTA styling on all delivered screens.

## Scope
`user_app/lib/core/widgets/buttons/common_button.dart`, `user_app/lib/app/theme/app_theme.dart`
(`filledButtonTheme`), `features/units_ownership/presentation/widgets/property_widgets.dart` (`PropertyAction`).

## Dependencies
BOS-011 UI-T01 (theme tokens).

## Implementation Requirements
Default CTA background = `colorScheme.primaryContainer`, foreground white (dark: onPrimaryContainer);
explicit `backgroundColor` overrides still win. `PropertyAction` fills its parent width.

## Acceptance Criteria
- Login "Continue with Phone" and OTP "Verify & Continue" render `#2563EB`.
- Save / Save & Add Another / Edit unit / Confirm are full-width, 48px tall, `#2563EB`.
- No behavior change; all existing tests pass.

## Test Requirements
Widget tests asserting CTA color and full width; `flutter analyze`, `flutter test`; emulator screenshots.

## UI Reference
`ui/stitch/mobile/02-login-screen`, `03-otp-verification-screen`, `11-add-edit-unit`, `12-unit-detail`;
`ui/stitch/design-system/DESIGN.md` (Buttons: primary #2563EB, 48px mobile).

## References
BOS-011 qa/QA.md.

## Out of Scope
Other layout changes.
