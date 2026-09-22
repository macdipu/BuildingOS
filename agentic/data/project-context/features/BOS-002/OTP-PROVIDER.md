# Development OTP and replaceable provider boundary

Date: 2026-09-22. Status: requirements/design proposal recorded; not implemented.
Latest user decisions:
- "for now mobile only and for otp use any free one and a layer so that vensor canbe chnaged any time"
- "for developmet use all 0," → development OTP is exactly `000000` (six zeros).

Working login scope: mobile-number OTP only, with Google deferred. The optional
clarification about "mobile only" has not received a separate answer.

## Current development provider

Select `DevelopmentOtpProvider` now. It sends no SMS, needs no vendor account/SDK,
and accepts `000000` only for an active development OTP challenge. This supersedes
the earlier Firebase-first development proposal. It simulates verification and does
not prove that the person controls a real phone number.

The flow still uses normal request/verify endpoints, challenge state, session issuance
and building authorization. It is not a client-side login shortcut or universal bearer
token. A verified development phone never automatically becomes an administrator.

Required behavior:
- Start a challenge for a syntactically valid normalized phone number; keep its opaque
  attempt ID, phone binding, expiry, attempt count and consumption state server-side.
- Verify against that attempt and the six-character string `000000`, retaining leading
  zeros. Reject other codes, missing/unknown attempts, phone mismatches, expired or
  consumed attempts and exhausted limits. Consume success atomically.
- Resend/cooldown/attempt-limit behavior uses the same application contract as a real
  provider. Numeric limits remain configurable technical-design inputs; a dummy provider
  must not silently disable them.
- Issue normal BuildingOS sessions through Identity after verification; gateway and
  business services continue verifying signed application tokens and membership.
- Use isolated development users/data. Tests identify the evidence as simulated rather
  than recording real phone ownership or production SMS-delivery acceptance.

## Environment boundary

Enable the provider only with explicit local/test configuration such as
`OTP_PROVIDER=development`. Non-local deployments must refuse startup if this provider,
a fixed-code override, or emulator verification is enabled. A production environment
must not become permissive merely because someone also adds a local profile.

Production selects a configured real provider or fails closed. Never fall back to
`000000` when a real provider is missing, unavailable, rate-limited or rejects a code.
The client must not auto-submit the code or select the trusted server verification mode.
No SMS or vendor account is created as part of this planning change.

## Proposed replacement interfaces

```text
Flutter phone/OTP screens and existing use cases
  -> PhoneOtpProvider (start, verify, resend; provider-neutral outcomes)
       -> BackendPhoneOtpAdapter (current development path)
            -> Identity OTP endpoints
                 -> OtpVerificationProvider
                      -> DevelopmentOtpProvider (000000; local/test only)
                      -> real managed-OTP/SMS adapter (later)
                 -> BuildingOS user/session handling
```

SDK-based providers such as Firebase may require a mobile adapter plus a server-side
`PhoneIdentityVerifier` for the resulting external proof. Their SDK types, verification
IDs and callbacks stay inside adapters. Screens/domain models must not import vendor
libraries. Server-side verifiers trust only configured providers and derive the verified
phone from trusted evidence, not a client assertion.

BuildingOS owns stable user UUIDs, membership, ownership records and session tokens.
Provider identity mappings remain separate. A replacement must preserve those IDs and
use an explicitly trusted re-verification/linking process; it cannot merge accounts just
because untrusted phone strings match. In-flight attempts stay bound to their original
provider/config version or expire explicitly during cutover.

Switching an installed backend adapter can be a configuration change. Adding a new
provider requires its adapter, contract tests and credentials; native SDK changes may
also require an app release. No business-screen or domain-data rewrite should be needed.

## Acceptance checks to include in implementation tasks

1. In local/test mode: start → verify `000000` → normal session; incorrect codes and
   unknown/expired/consumed attempts fail. Repeated/concurrent verification cannot reuse
   a consumed attempt. Treat the code as a string, not the integer zero.
2. In production/non-local mode: selecting the dev provider or fixed override fails
   startup. A real-provider failure never activates a simulated provider.
3. Replace the development adapter with a contract-test adapter without modifying UI,
   use cases, membership rules or domain user IDs. Bind attempts to the correct adapter.
4. Existing membership/object restrictions apply after OTP success; no first-user admin
   grant, arbitrary building access, or bypass of gateway/direct-service JWT verification.
5. Preserve redacted logging, storage-failure handling and concurrent session-refresh
   checks from BASELINE.md. Live-SMS delivery remains a separate future acceptance check.

## Future real-SMS candidate and researched cost boundary

Firebase Phone Authentication remains a researched candidate, not a dependency of the
current fixed-code development flow. Its test-number mode sends no SMS. Real SMS
requires Blaze billing; Google Identity Platform lists ten unbilled SMS per day and
charges beyond that. Recheck pricing/availability before selecting or activating live
service; "free development" does not authorize billing or chargeable SMS.

Official references checked 2026-09-22:
- [Firebase test numbers](https://firebase.google.com/docs/auth/flutter/phone-auth#testing)
- [Firebase SMS limits](https://firebase.google.com/docs/auth/limits#phone_number_sign-in_limits)
- [Identity Platform pricing](https://cloud.google.com/identity-platform/pricing)
- [Emulator isolation](https://firebase.google.com/docs/emulator-suite/connect_auth)

Initial SUPER_ADMIN and membership provisioning (Q-02) are now RESOLVED (see
DECISIONS.md D-02): deploy-time seed script/config for the first SUPER_ADMIN, no
password/admin panel, phone-based admin invitations for membership. This development
OTP choice does not implement that bootstrap; the seeded account still authenticates
through the same OTP challenge flow described in this document.
