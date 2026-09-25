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
| F6-T8 | Users + Support + Assisted-Onboarding screens, wired to F6-T2/T3/T4 (BOC-05/06/07) | F6-T2, F6-T3, F6-T4, F6-T6 |
| F6-T9 | System + Dashboard screens (BOC-08/09) | F6-T5, F6-T6 |
| F6-T10 | QA: browser walkthrough, release readiness | All |

Split further if a task cannot finish within the harness task budget. One task-finish
commit per bounded task.
