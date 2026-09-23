# TASK-006 — user_app: app-owned message for OTP start rate limit (429)

## Status: COMPLETED (2026-09-23) — run RUN-CF934E90E5A0492DA2C7D4021DD62CAF; evidence in ../REVIEW-TASK-006.md

Category: Mobile (`mobile-agent`). Scope: `user_app/` authentication feature only; no backend,
API, or flow change. Source: TASK-003 recorded follow-up ("app must handle 429
`OTP_RATE_LIMITED`; dedicated message is a follow-up"); operator "continue" after the
recommended next step, 2026-09-23.

## Current behavior (checked at `d1425d1`)

- `ApiClient` accepts statuses ≤500 (`validateStatus`), so a 429 from
  `POST /api/v1/auth/otp/start` becomes `Resource(messageCode: 429, message: body.message)`.
- `AuthRemoteDataSource.startOtp` throws `ServerException(response.message)`; the login and
  OTP-resend paths (`LoginScreenController.requestOtp`) show that server string verbatim:
  "Please wait before requesting another code". Readable, but server-owned text, unlike
  verify errors which map `OTP_*` codes via `otpErrorMessage`.

## Design

1. `AuthRemoteDataSource.startOtp`: `messageCode == 429` → `ServerException('OTP_RATE_LIMITED')`.
   Other failures unchanged. (The Resource for non-401 errors does not carry the body code; the
   start endpoint's only 429 is `OTP_RATE_LIMITED`.)
2. `login_screen_controller.dart`: add top-level `otpStartErrorMessage(String? code)`:
   `OTP_RATE_LIMITED` → "Too many code requests. Please wait before trying again."; otherwise
   the existing behavior (`code ?? 'Failed to send OTP'`). `requestOtp` uses it. Covers both the
   login Next button and OTP-screen resend.
3. English only, matching the existing OTP messages (none are localized today; no Bengali copy
   is invented here).

## Out of scope

Localizing OTP messages; showing remaining wait time (backend sends none); changing `ApiClient`.

## Acceptance

1. Start 429 → `Left(ServerFailure('OTP_RATE_LIMITED'))` (HTTP test).
2. `otpStartErrorMessage('OTP_RATE_LIMITED')` is readable, not a raw code; other inputs keep
   current behavior (unit test).
3. `sh scripts/verify-flutter.sh` passes (analyze + all tests incl. layer rules).
