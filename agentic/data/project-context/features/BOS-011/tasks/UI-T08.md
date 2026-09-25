# UI-T08 — Unit form Save & Add Another + restyle

## Category
Mobile

## Objective
Add BRD §49 `Save & Add Another`; restyle to mockup 11 as a single page.

## Scope
`unit_form_page.dart` (+ floor form restyle).

## Dependencies
UI-T01

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- Save & Add Another saves, keeps floor/type/default rate, clears number/area/bedrooms/notes, stays on form.
- Validation unchanged (unique number, positive area, valid type).

## Test Requirements
Widget test for Save & Add Another flow.

## UI Reference
`ui/stitch/mobile/11-add-edit-unit`; BRD §49. UI-only excluded (C-10): wizard steps, ownership step, parking, utility meters.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
Ownership assignment inside unit form.
