# UI-T02 — Splash screen + start routing

## Category
Mobile

## Objective
Implement BRD §39 splash: token/user/active-building resolution and three-way navigation.

## Scope
New `features/splash` (presentation, `ResolveStartDestination` use case); `AppPages.initial` -> splash.

## Dependencies
UI-T01

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- No token -> Login.
- Token + exactly one building and no pending invitation/application -> shell dashboard.
- Token + multiple buildings / pending invitation / application -> My Buildings.
- Expired/invalid token -> Login, stored session cleared.
- Shows logo + loading indicator per mockup.

## Test Requirements
Use-case unit tests for each branch; widget test for splash; route integration test updated.

## UI Reference
`ui/stitch/mobile/01-splash-screen`; BRD §39.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
Cross-building ownership portfolio view (BOS-008).
