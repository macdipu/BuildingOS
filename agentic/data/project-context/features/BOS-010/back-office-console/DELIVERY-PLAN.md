# F6 delivery sequence

Status: APPROVED sequence, 2026-09-25. Bounded executable tasks; no change to the
accepted TECH-SPEC.md scope.

| Order | Outcome | Dependencies |
|---|---|---|
| F6-T1 | `back-office-service` scaffold: module, Flyway V1 schema (`assisted_onboarding_session`, `support_session`, `elevated_approval_request`), Clean Architecture skeleton, health/readiness, gateway route | None |
| F6-T2 | `auth-service` internal endpoints: list users, assign/revoke platform role (BOC-05) | None |
| F6-T3 | `AssistedOnboardingSession` domain + API: start/list/detail/complete/cancel (BOC-06) | F6-T1 |
| F6-T4 | `SupportSession`/`SupportAccessGrant` + `ElevatedApprovalRequest` domain + API, action-relay authorization pattern (BOC-07) | F6-T1, F6-T2 |
| F6-T5 | Audit aggregation + system-health aggregation read APIs (BOC-08) | F6-T1 |
| F6-T6 | `buildingos_backoffice_web` scaffold: Next.js project, platform-JWT auth, nav shell (BOC-01/02) | None |
| F6-T7 | Buildings + Subscriptions screens, wired to existing F2/F5a APIs (BOC-03/04) | F6-T6 |
| F6-T7a | building-service `GET /api/v1/platform/buildings?status=&q=&page=&size=` (D-34) | F6-T7 lists; technical approval of the spec change |
| F6-T7b | subscription-service `GET /api/v1/platform/subscriptions?status=&plan=&page=&size=` (D-35) | F6-T7 lists; technical approval of the spec change |
| F6-T8 | Users + Support + Assisted-Onboarding screens, wired to F6-T2/T3/T4 (BOC-05/06/07) | F6-T2, F6-T3, F6-T4, F6-T6 |
| F6-T9 | System + Dashboard screens (BOC-08/09) | F6-T5, F6-T6 |
| F6-T10 | QA: browser walkthrough, release readiness | All |

UI reference (see `../../../ui/stitch/UI-INDEX.md`; layout only, BRD governs behavior):
F6-T6 nav shell ← `back-office/01` sidebar vs §149.2 nav; F6-T7 ← `back-office/02` (buildings /
applications) + `back-office/03` (subscriptions); F6-T8 ← no mockup (Users/Support) — plan from
§149.2/§149.11–149.12 + DESIGN.md, `back-office/02` for the onboarding view; F6-T9 ←
`back-office/01` (dashboard) + `back-office/04` (audit/system). Conflicts C-2 (automated
billing) and C-3 (kanban vs §149.4 states) are excluded from scope pending operator decision.

Progress: F6-T1 97d489a, F6-T2 975ea8d, F6-T6 f79e6d9, F6-T3 done; F6-T4 next.

Task files: `tasks/F6-T6.md`..`F6-T9.md` (planned 2026-09-25 with UI Reference). UI conflicts
C-2/C-3 are now decided (BRD wins; UI-INDEX). Planning questions (resolved 2026-09-25; F6-T7a/T7b change the approved TECH-SPEC, so they need
technical re-approval before implementation):

- **Q-F6-1 (RESOLVED D-34):** Buildings > Onboarding / Active / Suspended need a building list by status, but
  building-service only exposes `GET /api/v1/platform/buildings/{id}`. BOC-03 assumed no new
  endpoint. Proposal: add F6-T7a (BE) `GET /api/v1/platform/buildings?status=&q=&page=&size=`.
- **Q-F6-2 (RESOLVED D-35):** Subscriptions > "Building Subscriptions / Trials / Past Due / Suspended" assume
  building-level subscriptions and a list API; D-22 made subscriptions per user and there is no
  platform-wide list. Proposal: rename the section items to user subscriptions, add F6-T7b (BE)
  `GET /api/v1/platform/subscriptions?status=&plan=&page=&size=`, and show Trials / Past Due only
  once those states exist.

Split further if a task cannot finish within the harness task budget. One task-finish
commit per bounded task.
