# STORY-001: Live SMS OTP adapter and login brute-force controls

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-009 ([EPIC.md](../EPIC.md))

## Objective
Make phone+OTP login usable outside local/test by adding a production `SmsSender` adapter
behind the existing port, and complete §93 "Login brute-force controls" for OTP start/verify.
Unblocks the deferral recorded in BOS-010 RELEASE-TASK-003 ("production SMS login unavailable
until the vendor task").

## Scope
- auth-service infrastructure: vendor `SmsSender` adapter (HTTP client, timeouts, fail-closed),
  selected by configuration; `DevelopmentOtpProvider` stays local/test only (D-01) and must be
  impossible to enable under the production profile.
- Vendor credentials only via env/secret manager (§93).
- Brute-force controls: existing per-phone hourly start limit and 5 verify attempts
  (docs/AUTH_CONFIGURATION.md) extended with a per-client-IP start limit and a cooldown after
  exhausted attempts; all limits configurable (§41 "Retry limits, Rate limit").
- Flutter `authentication` feature (mockup `mobile/03-otp-verification-screen`, §41): show
  rate-limited / attempts-exhausted / expired states by error code (§128 "Clients must branch
  on code"); resend disabled during cooldown.
- Metrics: OTP sent/failed/rate-limited counters (feeds STORY-004).

## Acceptance Criteria
1. With the production profile and vendor configured, `POST` OTP start sends exactly one SMS
   through the vendor adapter per accepted challenge; the code is never logged or returned in
   the response (§93, §94 spirit; TASK-003-READINESS).
2. Starting with the production profile while `DevelopmentOtpProvider` is selected, or with
   vendor credentials missing, fails startup (fail-closed; D-01 "local/test only").
3. Vendor timeout or 5xx returns a stable error code and leaves no consumable challenge that
   the user cannot receive; no retry storm (bounded single retry or none per Q-BOS-009-01).
4. Exceeding the per-phone or per-IP start limit returns the rate-limit error code with
   HTTP 429 (§130 guidance, §128 code list extended at TECHNICAL); limits are configuration,
   not code (§41).
5. After verify attempts are exhausted the challenge cannot be verified, and a new challenge for
   that phone is refused until the configured cooldown passes (§93 brute-force).
6. Flutter OTP screen renders distinct messages for rate-limited, exhausted and expired codes;
   widget tests cover each (§97 Flutter widget tests).
7. Contract test against a stubbed vendor (WireMock or equivalent) covers success, timeout,
   4xx, 5xx (§97 adapter integration tests).
8. No vendor secret in Git; secret scan in CI passes (§93; STORY-007 scan).

## Tasks
Proposed task lanes only (no TASK files yet):
- BE: vendor `SmsSender` adapter + config + fail-closed startup guard - M
- BE: per-IP start limit + cooldown in StartOtp/VerifyOtp use cases - S
- Mobile: OTP error states by code + widget tests - S
- QA: vendor stub contract tests; production-profile startup negative tests - S

Size: M (uncertainty: high until vendor API is known - Q-BOS-009-01).

## Dependencies
- Q-BOS-009-01 (vendor), Q-BOS-009-02 (limit values). Soft: STORY-003 for distributed per-IP
  limits if Redis approved (Q-BOS-009-04).

## Out of Scope
- Google sign-in (C-11). Voice/WhatsApp OTP. Multiple vendor failover (unless Q-01 asks).

## References
BRD §41, §93, §94, §104 ("OTP state"), §128, §130; BOS-010 DECISIONS D-01;
features/BOS-010/RELEASE-TASK-003.md; features/BOS-010/TASK-003-READINESS.md;
docs/AUTH_CONFIGURATION.md; ui/stitch/UI-INDEX.md (mobile/03).
