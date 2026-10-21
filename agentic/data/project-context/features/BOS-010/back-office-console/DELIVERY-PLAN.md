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

Progress: F6-T1 97d489a, F6-T2 975ea8d, F6-T6 f79e6d9, F6-T3 a28b792; F6-T4 done (D-36),
F6-T4b relay planned; F6-T7 split into F6-T7.1 (application review, done) and F6-T7.2 (subscriptions, done). F6-T5 split: F6-T5a health (done), F6-T5b/c/d audit done (paging-edge gap noted in tasks/F6-T5b.md) (D-37).
F6-T7a/T7b done (e740842, d3dc815); F6-T7 list screens done (aa019cb; Playwright review-to-approve still open).
F6-T8 split: F6-T8a Assisted Onboarding screens (committed 77071e0); F6-T8b Support sessions + elevated approval screens
(committed 14b55ca; no audit-trail row links yet); F6-T8c-BE users proxy committed (7a16fa4) (back-office-service `/api/v1/platform/backoffice/users*`, token relay to auth-service F6-T2); F6-T8c Users screens built (Platform Users, Onboarding Agents, Support Agents; role assign/revoke SUPER_ADMIN only, with confirmation, none on own account). Gaps: Building Admins has no API (building-service memberships); no reason field on role changes (API has none); no per-user subscription/membership summary. (Was: BLOCKED: F6-T2 role list/assign/revoke exist only as internal auth-service endpoints and
back-office-service has no user proxy, so a BE task (F6-T8c-BE: `/api/v1/platform/backoffice/users*` relaying auth-service) was needed first.)
F6-T8a/T8b committed (77071e0, 14b55ca). F6-T9 screens committed (ed091b9): Dashboard (list-total cards), Audit Logs (server-side filters, nextUntil paging),
System Health, Entitlement Config (free tier), Platform Settings (not available). Gaps: audit sources record no before/after values (rows show action/actor/reason);
no entityId audit filter, so session rows do not deep-link to a single entity's trail; plans card is total plans, not by status. Playwright e2e added (827bf8f).
Gaps in F6-T8a: no onboarding-session history endpoint (detail shows current state only); "Assign Onboarding Agent" now links from an onboarding building's detail and from a just-approved application to the assisted-onboarding form, prefilled with the building id (an application record carries no building id, so a past approved application has no link).
F6-T5c verified: building-service and subscription-service `/internal/audit` shipped in 057b318 with integration tests (commit title was mangled). F6-T4b stays PLANNED: it needs a technical decision first (owning services authorize building members, not platform support users).

- **Q-F6-3 (RESOLVED D-36):** `SupportSession` lifecycle rules are not decided by D-10/BRD
  §149.12: (a) who may start a session and for whom (SUPPORT_AGENT self-start? admins?);
  (b) maximum duration; (c) who may end it and who sees which sessions; (d) whether a
  SUPER_ADMIN may approve a high-risk request on their own session; (e) which existing
  endpoint each high-risk/ordinary scope relays to (TECH-SPEC open question). Needs an operator
  decision (D-36) before F6-T4 is planned.

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

- **Q-F6-4 (RESOLVED D-37):** auth-service persists no platform-role grant/revoke audit rows (F6-T2 logs only), but TECH-SPEC/AU-01 expect stored rows and BOC-08's Audit Logs reads them. Options: add an auth-service role-assignment audit table in F6-T5b, or show only building/subscription/back-office audit rows for now.

**F6-T10 (2026-10-21, run `RUN-E75BB02D`, supersedes `RUN-8AAD6211` after a kit config change):**
baseline green at 441d13e (backend `mvn verify` 407/0; web lint/typecheck clean, vitest 59/59; Playwright
7/7 on the local stack; 21-route walkthrough all 200, no console errors). Code review **BLOCKED** —
see [REVIEW.md](REVIEW.md): R-1 HIGH (SUPPORT_AGENT cannot start a support session), R-2 proxy
allowlist bypass, R-3 dashboard onboarding-card count ≠ linked list, R-4 audit paging drops
same-timestamp rows, R-5..R-7 LOW. R-1 fix in progress per D-38 (service JWT, WIP commit edabf64:
auth `ArchitectureTest` still fails — presentation depends on `port.out` `IssuedToken`; back-office and
building tests not yet re-run). Open: R-4 needs a contract change + technical approval; last-SUPER_ADMIN
revoke guard is an operator question; R-2, R-3, R-5..R-7 need no decision.
