# F6 back-office console — code review (F6-T10)

Run `RUN-E75BB02D414A4AEFB153A1BF408C41F6` (supersedes `RUN-8AAD6211`), 2026-10-21.
Scope: `git diff 7370174 94b06a6 -- backend buildingos_backoffice_web`. Agent review; not human approval.

Verdict: **BLOCKED** — one HIGH defect breaks D-36a; two MEDIUM defects fail stated purpose/AC.

## Baseline evidence (at 441d13e)

| Check | Result |
|---|---|
| `mvn -B -f backend/pom.xml verify` | BUILD SUCCESS, 407 tests, 0 failures |
| web `npm run lint` / `typecheck` | exit 0 / exit 0 |
| web `npm test` (vitest) | 16 files, 59 tests passed |
| web `npx playwright test` (local stack) | 7/7 passed |
| Route walkthrough (21 console routes, SUPER_ADMIN) | all 200, no console errors |

## Confirmed defects

| # | Sev | Where | Defect | Fix direction |
|---|---|---|---|---|
| R-1 | HIGH | `back-office-service/.../startsupportsession/StartSupportSessionService.java:55,58`; `shared/infrastructure/client/AuthPlatformUserDirectory.java:33`; `BuildingServiceBuildingDirectory.java:29` | A SUPPORT_AGENT-only caller can never start a session: target/building existence checks relay the caller's token to auth `GET /internal/users/{id}` and building `GET /api/v1/platform/buildings/{id}`, both SUPER_ADMIN/PLATFORM_ADMIN only; the 403 is mapped to 503 DEPENDENCY_UNAVAILABLE. Tests miss it (`DownstreamStubs` does not enforce roles). | **Needs a technical decision** (Q-F6-5): narrow existence lookup allowed for SUPPORT_AGENT in auth/building, or a service credential for this lookup only. Either way map downstream 403 → 403. |
| R-2 | MEDIUM | `buildingos_backoffice_web/src/app/api/gateway/[...path]/route.ts:10-19`, `src/lib/gateway.ts:10-18` | Proxy allowlist bypass: segments are decoded then joined unencoded, so `/api/gateway/platform/%252e%252e/me` passes `isAllowedPath` and is normalized upstream to `/api/v1/me`. Verified live: reaches the gateway (500 from upstream) instead of the proxy's 404. User's own token only; no privilege gain. | `encodeURIComponent` each segment; reject `.`/`..`/`%`/`?`/`#` segments; re-check `new URL(...).pathname` prefix. |
| R-3 | MEDIUM | `buildingos_backoffice_web/src/components/DashboardScreen.tsx:38` | "Onboarding sessions awaiting start" counts `status=ASSIGNED` but links to the unfiltered list — fails F6-T9 AC "numbers equal the totals of their linked lists". | Link `?status=ASSIGNED`; screen accepts an initial status (as it does `buildingId`). |
| R-4 | MEDIUM | `back-office-service/.../auditlog/domain/model/AuditPage.java:30` (+ all four `/internal/audit` sources) | Audit paging drops rows sharing the boundary `occurredAt` (`until` exclusive). Same-instant rows are routine (support session + its high-risk scope transitions). Known F6-T5b gap, now rated a data-visibility defect. | Composite `(occurredAt, id)` cursor through every source. Contract change → technical approval. |
| R-5 | LOW | `ListPlatformBuildingsService.java:19`, `ListPlatformSubscriptionsService.java:21`, back-office `ListSupportSessionsService.java:35`, `ListElevatedApprovalsService.java:27`, `ListOnboardingSessionsService.java:35` | `page*size` int overflow → negative OFFSET → 500. | `Math.multiplyExact` on long or bound `page` in `Page.validate`. |
| R-6 | LOW | `buildingos_backoffice_web/src/proxy.ts:22`, `src/lib/api.ts` | Session expiring mid-use shows "Error code: UNAUTHENTICATED" instead of redirecting to sign-in (F6-T6 AC). | On 401 in `call()`, navigate to `/login`. |
| R-7 | LOW | `buildingos_backoffice_web/src/proxy.ts:14-16` | No-role path redirects to `/denied` without clearing the cookie (F6-T6 "is denied and is signed out"); near-unreachable. | Delete the session cookie on that redirect. |

## Questions (not defects)

- Any SUPER_ADMIN may revoke another; the last two can strip each other. Last-SUPER_ADMIN guard intended?
- `lifecycle_transition` has no `occurred_at` index; unfiltered audit sort will degrade.
- `entityType` containing `{...}` is expanded as a URI template in `HttpRemoteAuditLog` → source shown unavailable instead of 400.
- F6-T8 asks for a role-change reason and a user-detail membership/subscription summary; neither contract nor UI has them (already listed as gaps in DELIVERY-PLAN.md).
- Audit `older()` does not clear a previous failure on success (`AuditLogScreen.tsx:94`).

## Checked, no defect

Role gating on assign/revoke/audit/health (SUPER_ADMIN, self-change refused server-side); no gateway route to `/internal/**`;
users proxy relays the caller's token; four-eyes approval with ordered `FOR UPDATE` locks and SKIP LOCKED expiry; onboarding
transitions re-checked under lock (409); SQL filters bound and allow-listed; auth audit row written in the role-change
transaction. Web: httpOnly SameSite=Strict session cookie, server-only API base URL, no open redirect, role
assign/revoke SUPER_ADMIN-only with confirmation and own-account block, upstream errors shown as codes never numbers.
