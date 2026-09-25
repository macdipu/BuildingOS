# UI-T03 — Per-role bottom navigation

## Category
Mobile

## Objective
Replace starter AppShell with BRD §37 per-role nav sets (D-02).

## Scope
`app/shell/*`: NavSet config (Admin/Manager, Owner, Committee, Tenant), resolution from active building roles.

## Dependencies
UI-T01, UI-T02 (Q-01/Q-02 resolved)

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- BUILDING_ADMIN sees Dashboard/Finance/Units/Work/More; OWNER sees Dashboard/Properties/Payments/Community/More (labels localized en/bn).
- Multi-role users get the set chosen by Q-01 precedence.
- Switching active building re-resolves the nav.
- Unbuilt-module tabs behave per Q-02.
- Units tab opens unit list; Properties opens My Buildings.

## Test Requirements
Unit tests for nav resolution per role combination; widget test for shell per role.

## UI Reference
`ui/stitch/mobile/04-my-buildings-selector` .. `16-owner-detail` bottom bar (style only); BRD §37. UI-only excluded: single shared nav bar (C-1).

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
Dashboard content (BOS-008); Tenant/Committee roles in backend.
