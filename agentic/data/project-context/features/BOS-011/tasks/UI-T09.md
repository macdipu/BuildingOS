# UI-T09 — Unit detail, ownership, transfer, members restyle

## Category
Mobile

## Objective
Restyle delivered unit detail / ownership / transfer / members / building-application screens.

## Scope
`unit_detail_page`, `ownership_page`, `ownership_form_page`, `transfer_files_page`, `members_page`, `building_application/presentation/pages/*`.

## Dependencies
UI-T01

## Implementation Requirements
See [TECH-SPEC.md](../TECH-SPEC.md). Clean Architecture per `docs/ARCHITECTURE.md`; reuse existing shared widgets/theme.

## Acceptance Criteria
- Unit detail and transfer match mockups 12/14 layout; others follow theme only (no mockup).
- No behavior change; NID/personal data stays masked per §94 where shown.

## Test Requirements
Existing widget tests pass/updated.

## UI Reference
`ui/stitch/mobile/12-unit-detail`, `14-ownership-transfer`; BRD §50-54, §127. UI-only excluded (C-10): transfer OTP gate, legal-instrument type.

## References
BOS-011 [CR.md](../CR.md), [DECISIONS.md](../DECISIONS.md); `agentic/data/project-context/ui/stitch/UI-INDEX.md`.

## Out of Scope
New tabs (Tenant/Lease/Rent - BOS-003/004).
