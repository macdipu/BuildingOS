# BOS-010 F6 back-office console: QA

Date: 2026-10-22. QA agent, live local stack. No application code was changed.

**Verdict: QA_PASS.** All F6 checks (Playwright, R-1 to R-7, role walkthrough) pass. There is one LOW
defect, and it is in BOS-001 gateway/platform-web code that F6 did not introduce (D-1 below).

## Environment

- Commit: `b607dbd` (HEAD, main). The images include R-1..R-7, N-1..N-3 and R-4 (keyset cursor).
- Build: `mvn -B -q -f backend/pom.xml package -DskipTests` exited 0. Then
  `docker compose -f infra/local/compose.yaml --env-file infra/local/.env up -d --build`, then
  `--force-recreate api-gateway backoffice-web`. The gateway container had been running an older,
  untagged image.
- Images built 2026-10-22 11:46 +06: auth-service, building-service, subscription-service,
  back-office-service, backoffice-web (`buildingos/<svc>:local`). api-gateway was not rebuilt: its image is
  from 2026-10-21 21:18, and no api-gateway commit came after it. The gateway container was recreated
  on `buildingos/api-gateway:local`.
- All 9 containers were up. Every service with a healthcheck reported healthy. Web answered `:3000` (200).
- Test users (dev OTP `000000`, created by signing in through `/api/v1/auth/otp/*`, roles granted by
  SUPER_ADMIN through the console proxy `POST /api/gateway/platform/backoffice/users/{id}/platform-roles`, all 200):

| Phone | Role | User id |
|---|---|---|
| 01306999005 | SUPER_ADMIN (seed) | — |
| 01791110001 | SUPPORT_AGENT | 2550add5-3245-4153-ae20-55657d298878 |
| 01791110002 | PLATFORM_ADMIN | 7463f27f-2a9f-4a2e-be24-a1ab6802c0ef |
| 01791110003 | ONBOARDING_AGENT | a7f9ef96-4dd1-42a1-9ac8-3437eb33a999 |
| 01791110004 | SUBSCRIPTION_ADMIN | 2839852a-f50e-46a3-960d-0b3f45f7c3b3 |
| 01791110005 | none | f22befda-7243-4eb6-a3a9-b25cb8d6440e |

Tokens are elided below as `$TOK_<role>`.

## Results

| # | Check | Result |
|---|---|---|
| 1 | `npx playwright test` (web e2e, local stack) | **PASS**, 7/7 |
| 2a | R-1 SUPPORT_AGENT starts a support session (service token) | **PASS** |
| 2b | R-2 `%252e%252e` path traversal → 404 at the proxy | **PASS** |
| 2c | R-3 dashboard "awaiting start" count = linked list total | **PASS** |
| 2d | R-4 audit keyset paging over same-timestamp events | **PASS** |
| 2e | R-5 huge `page` → 400 | **PASS** |
| 2f | R-6 / R-7 expired/invalid cookie → /login; no role → /denied + cookie cleared | **PASS** |
| 3 | Role walkthrough, 5 roles, every nav route | **PASS** |

### 1. Playwright

`cd buildingos_backoffice_web && npx playwright test` gave 7 passed in 12.0s: setup sign-in,
2× dashboard-audit, 4× onboarding-flow. This matches the REVIEW.md baseline of 7/7.

### 2a. R-1: support session started by a SUPPORT_AGENT-only user

Calls go through the web proxy as SUPPORT_AGENT (`Cookie: bos_session=$TOK_SUPPORT_AGENT`) to
`POST /api/gateway/platform/backoffice/support-sessions`:

| Body | Status | Response |
|---|---|---|
| target f22befda (existing), building 059fbc1c (existing), scopes VIEW_MEMBERS + REVERSE_PAYMENT | **201** | `status: ACTIVE`, session e8a503a9-…; REVERSE_PAYMENT became a PENDING elevated approval |
| target `00000000-0000-4000-8000-000000000001` (unknown) | **422** | `TARGET_USER_NOT_FOUND` |
| building `00000000-0000-4000-8000-000000000002` (unknown) | **422** | `BUILDING_NOT_FOUND` |
| target only, no building | **201** | ACTIVE |

The lookups used the service token, not the caller's token:
- With the caller's own token, the same downstream lookups are refused. `GET :8081/internal/directory/users/f22befda…`
  with `$TOK_SUPPORT_AGENT` returned **403** ACCESS_DENIED. `GET :8082/api/v1/platform/buildings/059fbc1c…` with
  `$TOK_SUPPORT_AGENT` returned **403** ACCESS_DENIED. The lookups inside the session start still succeeded.
- I restarted back-office-service to clear its cached service token, then started one session. The logs show,
  in order: auth-service `POST 200` at 05:53:39.259 (the `/internal/service-tokens` exchange), auth-service
  `GET 200` at .271 (directory lookup), building-service `GET 200` at .284, back-office `POST 201` at .307.
  `CorrelationFilter` logs only the method and status, not the path, so the exchange is identified by its
  order and by the 403s above. No secret or token appears in any log.

### 2b. R-2: proxy path traversal

`curl -H "Cookie: bos_session=$TOK_SA" http://127.0.0.1:3000/api/gateway/platform/<p>`:

| `<p>` | Result |
|---|---|
| `%252e%252e/me` | 404 `{"success":false,"code":"NOT_FOUND"}` |
| `%2e%2e/me` | 404 |
| `..%2fme` | 404 |
| `%252e%252e%252fauth/otp/start` | 404 |

### 2c. R-3: dashboard count

I seeded two onboarding sessions, ASSIGNED to the ONBOARDING_AGENT (`POST …/onboarding-sessions`, 201 ×2).
- API: `GET /api/v1/platform/backoffice/onboarding-sessions?status=ASSIGNED` returned `meta.total = 2`.
- Browser as SUPER_ADMIN: the card "Onboarding sessions awaiting start" shows **2** and links to
  `/support/assisted-onboarding?status=ASSIGNED`. Clicking it opens the list with the status filter
  preselected to `ASSIGNED`, showing exactly the 2 rows (buildings 43413a9c…, 059fbc1c…).

### 2d. R-4: audit paging with same-timestamp events

I started one session with VIEW_MEMBERS plus 4 high-risk scopes. That wrote 5 transitions (1 SUPPORT_SESSION
NONE→ACTIVE, 4 ELEVATED_APPROVAL_REQUEST NONE→PENDING), all at `occurredAt 2026-10-22T05:54:04.030758Z`. Three
more timestamps were each shared by 2 events. The script (scratchpad `r4.py`) follows `nextUntil`/`nextUntilId` from
`GET /api/v1/platform/backoffice/audit?limit=N` and compares the result with a single `limit=200` fetch
(35 events from all 4 sources, `unavailableSources: []`):

| limit | pages | seen | unique | duplicates | missing | same order as full |
|---|---|---|---|---|---|---|
| 1 | 36 | 35 | 35 | 0 | 0 | yes |
| 2 | 18 | 35 | 35 | 0 | 0 | yes |
| 3 | 12 | 35 | 35 | 0 | 0 | yes |

Validation: `untilId` without `until` returns 400 "untilId requires until". `limit=201` returns 400. The same response also
comes through the web proxy (`/api/gateway/platform/backoffice/audit?limit=200`, 35 items).

### 2e. R-5: page bounds

`GET /api/v1/platform/buildings?page=2147483647&size=100` (gateway, `$TOK_SA`) returned **400** INVALID_REQUEST
"page is too large". Through the proxy, `/api/gateway/platform/buildings?page=2147483647&size=100` also returned **400**.
Other inputs: `page=21474837` 400, `page=-1` 400, `size=101` 400, `page=0&size=100` 200. The same huge-page call
returns 400 on subscriptions, backoffice/users, onboarding-sessions, support-sessions, elevated-approvals and
building-applications.

### 2f. R-6 / R-7: session cookie handling

`curl -D - -H "Cookie: bos_session=<x>" http://127.0.0.1:3000/<route>`:

| Cookie | Route | Result |
|---|---|---|
| expired JWT (exp in the past) | /buildings/active | 307 → `/login`, `Set-Cookie: bos_session=; Expires=Thu, 01 Jan 1970` |
| not a JWT | /buildings/active | 307 → `/login`, cookie deleted |
| none | /buildings/active | 307 → `/login` |
| valid token, no platform role (01791110005) | / and /system/audit-logs | 307 → `/denied`, `Set-Cookie: bos_session=; Expires=Thu, 01 Jan 1970` |
| expired JWT | `/api/gateway/platform/buildings` | 401 `UNAUTHENTICATED` |

- R-6 mid-use case, checked in a browser: the cookie held an unexpired JWT with a bad signature. The page
  shell rendered (200), the first API call got 401 from the gateway, and the client went to `/login`.
- No-role sign-in through the console (`POST /api/auth/otp/verify`) returned 403 `NO_PLATFORM_ROLE` and set no cookie.

### 3. Role walkthrough

A Playwright script (scratchpad `walk.cjs`, raw output `walk.json`) set each role's token as `bos_session`. It
loaded `/` and every route the nav renders, waited for network idle, and captured HTTP status, final URL,
`Error code:` notices, browser console errors and page errors, and `/api/*` responses ≥ 400. It then tried every
route the role should not see.

| Role | Nav routes loaded | 200 | Error notices | Console errors | API ≥400 | Placeholder (NotAvailable) | Hidden routes |
|---|---|---|---|---|---|---|---|
| SUPER_ADMIN | 21 (incl. /) | 21 | 0 | 0 | 0 | 4 | — |
| PLATFORM_ADMIN | 17 | 17 | 0 | 0 | 0 | 3 | 4 → 404 |
| SUPPORT_AGENT | 3 | 3 | 0 | 0 | 0 | 0 | 18 → 404 |
| ONBOARDING_AGENT | 2 | 2 | 0 | 0 | 0 | 0 | 19 → 404 |
| SUBSCRIPTION_ADMIN | 4 | 4 | 0 | 0 | 0 | 1 | 17 → 404 |

The rendered nav links match `src/lib/nav.ts` `visibleNav` for each role. The placeholders are the intended
NotAvailable notes: buildings/archived, users/building-admins, subscriptions/suspended-cancelled and system/settings.

## Defects

| ID | Severity | Location | Description / repro |
|---|---|---|---|
| D-1 | LOW (not F6; BOS-001 gateway/platform-web) | `backend/platform-web/src/main/java/com/buildingos/platform/web/exception/PlatformErrorHandler.java:39-45` (catch-all `Exception`) | An authenticated request to a path with no gateway route returns **500 INTERNAL_ERROR** instead of 404. Spring's `NoResourceFoundException` is caught by the catch-all and logged at ERROR with a stack trace. Repro: `curl -H "Authorization: Bearer $TOK_SA" localhost:8080/api/v1/nope` returns 500. Same for `/internal/directory/users/x` and `/internal/service-tokens`. `/internal/**` is still not exposed: nothing is routed and nothing leaks. The cost is a wrong status and noisy logs. The console proxy is not affected, because R-2 rejects these paths before they reach the gateway. |

Observations (not defects against a stated AC):
- O-1: After the R-6 mid-use redirect to `/login`, the bad `bos_session` cookie is still set, because the proxy
  matcher skips `/login`. It is harmless: the next sign-in overwrites it, and any console route with it ends at `/login` again.
- O-2: Audit items for support-session transitions show `buildingId: null` even when the session has a building.
  No AC in scope requires it.

## Not verified

- The back-office/auth request logs carry no path, so the `/internal/service-tokens` exchange is identified by
  order and by the caller-token 403s, not by a logged URL.

The stack is left running, with the seeded users and sessions above.
