# STORY-002: Refresh token rotation and session revocation

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Satisfy §93 "Short-lived access token" + "Refresh token rotation" so users stay signed in
without long-lived access tokens, and sessions can be revoked. docs/AUTH_CONFIGURATION.md
currently lists refresh tokens and session revocation as not implemented.

## Scope
- auth-service: issue a refresh token with the access token at OTP verify; rotate on refresh;
  reuse of a rotated token revokes the whole token family; logout revokes the family.
- Refresh tokens stored hashed (never plaintext) in auth_db; Flyway migration (§99).
- Revocation on platform-role revoke is decided in Q-BOS-009-03 (recommended: revoke).
- api-gateway route for the refresh/logout endpoints (public route, rate-limited by STORY-003).
- Flutter `authentication` + core HTTP client: silent refresh on expiry/401, single in-flight
  refresh, logout calls revoke; tokens stay in secure storage (docs/ARCHITECTURE.md
  "Secrets ... stored with `secure: true`").
- Back-office web: same refresh/logout flow in its auth proxy (D-09 separate web stack).
- OpenAPI contract update in `contracts/openapi/platform.yaml` (§98).

## Acceptance Criteria
1. OTP verify returns access + refresh token; access TTL remains configurable (existing
   `AUTH_ACCESS_TOKEN_TTL`), refresh TTL configurable (§93).
2. Refresh with a valid token returns a new pair and invalidates the presented token
   (rotation, §93).
3. Presenting an already-rotated refresh token is rejected and revokes every token of that
   family; the event is logged (no token value) and audited as a security event (§93 "Audit
   privileged operations", §114 role/security changes).
4. Logout revokes the family; subsequent refresh returns `AUTH_REQUIRED` (§128).
5. Concurrent refresh of the same token yields at most one successful rotation (§129
   concurrency; idempotency spirit of §16).
6. Refresh tokens are stored as hashes; DB dump does not reveal usable tokens (§93).
7. Flutter: an expired access token triggers one refresh and retries the original request
   once; failure routes to login; unit + controller tests (§97 Flutter).
8. Back-office web: same behaviour, covered by its existing test runner.
9. Contract check (`scripts/check-contracts.py`, STORY-006 breaking-change gate) passes with
   the additive change.

## Tasks
Proposed task lanes only:
- BE/DB: refresh_token table + issue/rotate/revoke use cases (one use case per action) - M
- BE: gateway routes + OpenAPI - S
- Mobile: token refresh interceptor + logout - M
- Web: back-office refresh/logout - S
- QA: rotation/reuse/concurrency/security tests - S

Size: L (uncertainty: medium; contract change touches both clients).

## Dependencies
- Q-BOS-009-03 (TTL values, role-revoke behaviour). Soft: STORY-003 rate limit on refresh.

## Out of Scope
- Automated signing-key rotation (docs/AUTH_CONFIGURATION.md: remains manual).
- Device/session management UI (BRD has no such screen).

## References
BRD §8.1, §93, §114, §128, §129; docs/AUTH_CONFIGURATION.md; docs/ARCHITECTURE.md;
BOS-010 DECISIONS D-09, D-38.
