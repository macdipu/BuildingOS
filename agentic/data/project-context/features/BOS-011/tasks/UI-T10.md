# UI-T10 — Visual + regression QA

## Category
QA

## Objective
Verify BOS-011 on device against mockups and run full regression.

## Scope
user_app + building-service.

## Dependencies
UI-T01..UI-T09

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- Device preview screenshots for splash, login, OTP, My Buildings, unit list, unit form, unit detail, transfer, nav per role; each compared with screen.png, deviations listed.
- `flutter analyze`, `flutter test`, `mvn -pl building-service verify` green.

## Test Requirements
As above; evidence under `features/BOS-011/qa/`.

## UI Reference
All mapped `ui/stitch/mobile/*` folders.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
UAT sign-off (operator).
