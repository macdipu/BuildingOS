# UI-T07 — Unit list filter/search/sort + restyle

## Category
Mobile

## Objective
Add Owner/Type/Floor filters, search, sort to unit list; restyle to mockup 10.

## Scope
`building_units_page.dart` + data source/repository params.

## Dependencies
UI-T01, UI-T06

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- Filter sheet Type/Floor/Owner, search box, sort menu wired to UI-T06 params.
- `+ Add Unit` and row tap -> Unit Detail kept; batch/floor actions kept.
- Empty/loading/error states present.

## Test Requirements
Controller/repository unit tests; widget tests for filter/search/sort.

## UI Reference
`ui/stitch/mobile/10-unit-list`; BRD §48. Excluded until BOS-003/004: Tenant/Occupancy/Rent/Maintenance columns.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
Occupancy/due filters.
