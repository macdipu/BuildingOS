# BOS-010 F6 — back-office console: local/test release

Status: READY for local/test, approved 2026-10-22. Run `RUN-E75BB02D414A4AEFB153A1BF408C41F6`.
Release candidate: `b607dbd` (code); QA evidence `3ee2503`. No deployment is performed by this record.

## Scope

`back-office-service` + `buildingos_backoffice_web` (BOC-01..09): Dashboard, Buildings, Users, Subscriptions,
Support (assisted onboarding, support sessions, elevated approvals), System (audit logs, health, entitlements).
F6-T1..T10 delivered; F6-T4b (support action relay) is not part of this release (post-MVP backlog).

## Verification

- [Review](REVIEW.md): R-1..R-7 and N-1..N-3 fixed, R-4 fixed under D-39, N-4 accepted; re-review READY.
- Backend `mvn -B -f backend/pom.xml verify` at R-4: platform-web 5, auth 100, building 129, subscription 40,
  back-office 93, gateway 53 — 0 failures. Web lint 0, typecheck 0, vitest 18 files / 68 tests.
- [Live-stack QA](qa/QA.md) at `b607dbd`: QA_PASS — Playwright 7/7, every fix verified live, five-role
  walkthrough (SUPER_ADMIN 21, PLATFORM_ADMIN 17, SUPPORT_AGENT 3, ONBOARDING_AGENT 2, SUBSCRIPTION_ADMIN 4
  routes) with no errors.

## Known limitations

- D-1 LOW (BOS-001 gateway/platform-web, not F6): an unrouted signed-in path returns 500 instead of 404
  (`PlatformErrorHandler` catch-all). Backlog for BOS-009 STORY-003.
- Audit entries for support-session events show `buildingId: null` (observation).
- `lifecycle_transition` has no `occurred_at` index; `AuditLogScreen.older()` does not clear a previous error.
- Audit paging with `until` alone keeps the old exclusive behaviour; complete paging needs `until` + `untilId`.

## Configuration and migration checklist

- `BACKOFFICE_SERVICE_CLIENT_SECRET` (≥ 32 chars, same value for auth-service and back-office-service) in
  `infra/<env>/.env`; `AUTH_SERVICE_TOKEN_TTL` optional (default 5 min). Never commit the value.
- Flyway forward migrations: auth-service V4 `platform_role_audit`; back-office-service V1–V3.
- Run auth, building, subscription, back-office, gateway and backoffice-web from the same commit
  (the directory endpoints and the `untilId` audit parameter must exist before back-office calls them).

## Rollback and monitoring

- Roll back by redeploying the previous images; migrations are additive (no destructive change).
- If back-office cannot obtain a service token, support-session start fails closed with 503
  `DEPENDENCY_UNAVAILABLE` (N-1 fix); check the shared secret and auth-service `/internal/service-tokens`.
- Watch back-office logs for `DEPENDENCY_UNAVAILABLE` and the gateway for 5xx.

## Approval

The operator (macdipu) answered **"Approve release (Recommended)"** on 2026-10-22 (AskUserQuestion) to: "Do you approve the F6 local/test release?" Local/test only; no production deployment is implied.
