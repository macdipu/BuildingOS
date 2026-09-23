# TASK-002 — Flutter (`user_app`) phone+OTP login integration

## Status: IMPLEMENTED; 22 Flutter tests pass; strict analysis clean; live flow QA outstanding

2026-09-23 continuation: see [review/QA checkpoint](../REVIEW-QA.md). Added nine
HTTP contract/persistence tests using the real client/repositories and a loopback
server, with an in-memory device-storage boundary. No mocking dependency needed.
The earlier verification account below is historical: analysis was not a clean
gate (informational lints return exit 1). Recovery continuation cleared all
145 infos (`flutter analyze`: no issues). Live device/back-end flow remains unverified.

Wires the TASK-001 backend OTP endpoints into the existing GetX auth feature
(`user_app/lib/features/authentication/`). Scope is the login flow only — the
existing `forgot_pin_*` stub flow, refresh-token robustness bugs, and OTP/token
log redaction noted in BASELINE.md are explicitly out of scope for this task.

## Backend contract (from TASK-001, `AuthController`)

- `POST /api/v1/auth/otp/start` `{phone}` -> `ApiEnvelope<{attemptId, expiresAt}>`
- `POST /api/v1/auth/otp/verify` `{attemptId, phone, code}` -> 200
  `ApiEnvelope<{accessToken, tokenType, expiresInSeconds, user:{id, phone, platformRoles[]}}>`
  or 401 `ApiError{code: "OTP_<REASON>", ...}`
- Both routes are public through the gateway (no bearer token); `phone` is the
  local format (`01XXXXXXXXX`, i.e. `PhoneNumber.withoutCountryCode`), matching
  the existing `AuthLoginReq` convention and the TASK-001 seed phone.
- No refresh token is issued (local RSA issuer, TASK-003 will add production
  issuance) — `UserInfo.refreshToken` is `null` for OTP-issued sessions.

## Acceptance criteria

1. `AuthRepository` gains `startOtp`/`verifyOtp` methods alongside the existing
   password `login`, implemented by `AuthHttpImpl` (HTTP) and decorated by
   `AuthCacheImpl` (session persistence via `SharedPreferenceConstant.customerInfo`
   + `authHttpImpl.jwtUpdated()`), matching the existing pattern.
2. `UserInfo` carries the platform roles returned by `/verify` without breaking
   existing `fromJson`/`fromApiJson`/`toJson` callers.
3. A phone-entry step calls `/start` and navigates to an OTP-entry step (reusing
   `CommonPinInputField` / `OtpResendButton`, matching `ForgotPinOtpVerify`'s
   layout) that calls `/verify`.
4. Loading state during both calls and every backend `OTP_*` rejection reason is
   surfaced as a user-facing error (via `BaseController.doAction`/`CustomSnackbar`),
   not just a generic failure message.
5. On verify success, the session is persisted and navigation lands past login
   (same post-login destination as the existing password flow) without a stale
   password-login codepath left dangling as the default entry point.
6. `flutter analyze` clean; existing tests still pass; add at least
   repository/use-case-level test coverage for the new OTP methods (success +
   at least one rejection path).

## Implementation notes

- Additive: old password `login()`/`DoLoginUseCase`/`AuthCacheImpl.login` path is
  intact but no longer reachable from any screen (LoginScreen's CTA now starts
  the OTP flow) — deleting it is a separate product decision, out of scope here.
- Required one unplanned, minimal, backward-compatible change: `ApiClient`'s 401
  branch was discarding `response.data` before throwing `UnauthorizedException`,
  which made per-reason `OTP_<REASON>` error messages (criterion 4) structurally
  unreachable. `ApiException`/`UnauthorizedException` gained an optional `body`
  param (default `null`); `AuthHttpImpl.verifyOtp` reads `e.body['code']`.
- New `UserInfo.platformRoles` (default `const []`) and `UserInfo.fromOtpVerifyJson`
  unwrap the `/verify` response's `data.user.platformRoles`; existing
  `fromJson`/`fromApiJson`/`toJson` callers unaffected.
- Original test coverage was use-case level (fake `AuthRepository`), not `AuthHttpImpl`/
  `AuthCacheImpl` directly — those need a real `ApiClient`/`PreferenceCache`
  (Dio/platform channels) to construct and there's no mocking library in the
  project; adding one was judged out of scope for this task.

## Verification

Implemented by a fork, independently re-verified in-session (not just trusting
the fork's report): re-ran `flutter analyze` (clean, 145 pre-existing info-level
lints, 2 new `unawaited_futures` infos in the new screens matching the existing
`forgot_pin_*` style) and `flutter test` (13/13 passed, including the 6 new
OTP use-case tests) myself from a clean shell, and read every changed/created
file's diff against the backend contract (`AuthController`,
`VerifyOtpChallenge.Result.Rejected.Reason` enum: all 6 reasons mapped 1:1 in
`otpErrorMessage`) and against the pre-TASK-002 `login_screen.dart` to confirm
the post-verify navigation target (`AppRoutes.appShell`) matches exactly what
the old password-login success path used, not an invented destination.

## Explicitly not covered

`forgot_pin_*` PIN-reset stub flow, `api_client.dart` refresh-token/concurrency
bugs noted in BASELINE.md, request/response log redaction, TASK-003 production
token issuance.
