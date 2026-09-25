# UI-T01 — Stitch theme tokens

## Category
Mobile

## Objective
Replace user_app brand theme with DESIGN.md tokens (D-01).

## Scope
`app/theme/{color_schemes,text_theme,app_dimensions,app_theme,theme_extensions}.dart`; currency text styles extension. Dark scheme per Q-03.

## Dependencies
None

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- All ColorScheme roles equal DESIGN.md hex values in light mode.
- Headline/display use Plus Jakarta Sans, body/label use Inter, sizes/weights per DESIGN.md.
- Radii/spacing tokens exposed via app_dimensions; no screen adds hard-coded colors.
- Existing screens render without layout overflow.

## Test Requirements
Theme unit test asserting token values; update color/style-asserting widget tests; `flutter analyze` + `flutter test` green.

## UI Reference
`ui/stitch/design-system/DESIGN.md`; all `ui/stitch/mobile/*` for visual check. BRD §109 (Bangla/BDT still render).

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
Per-screen layout changes (other tasks).
