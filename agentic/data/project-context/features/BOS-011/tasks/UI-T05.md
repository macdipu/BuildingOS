# UI-T05 — My Buildings restyle + access warning

## Category
Mobile

## Objective
Restyle My Buildings to mockup 04 and ensure BRD §42 subscription/access warning.

## Scope
`features/units_ownership/presentation/pages/property_home_page.dart`.

## Dependencies
UI-T01

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- Layout per mockup; building cards show name, role, status.
- Subscription/access warning shown when relevant and user authorized (§42); if no data source exists, record an open question, do not invent.

## Test Requirements
Widget tests for warning visible/hidden.

## UI Reference
`ui/stitch/mobile/04-my-buildings-selector`; BRD §42.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
New subscription APIs.
