# UI-T04 — Login + OTP restyle

## Category
Mobile

## Objective
Restyle delivered login and OTP screens to mockups 02/03.

## Scope
`features/authentication/presentation/pages/{login_screen,login_otp_verify_screen}.dart`.

## Dependencies
UI-T01

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- Layout matches mockup regions; behavior (phone validation, OTP, 429 message, locale toggle) unchanged.
- No Google sign-in button (C-11).

## Test Requirements
Existing auth widget tests pass/updated; device preview vs screen.png.

## UI Reference
`ui/stitch/mobile/02-login-screen`, `03-otp-verification-screen`; BRD §40-41. UI-only excluded: Continue with Google.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
Auth logic changes.
