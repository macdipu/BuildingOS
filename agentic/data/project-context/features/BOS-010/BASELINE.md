# BOS-010 scoped baseline

Observed 2026-09-22, reusing the verified BOS-002 baseline (unchanged since — no code
was written in BOS-001/BOS-002 beyond the BOS-001 platform skeleton). Scope: identity/
platform-role/building-lifecycle foundations, gateway security/contracts, Flutter
login/cache/HTTP integration, and the new subscription/back-office surfaces which have
no prior implementation at all.

## Confirmed reusable foundation

BOS-001 is COMPLETED with explicit local milestone approval: 37 passing backend tests,
database bootstrap/isolation, migration reruns, Kafka record persistence across broker
recreation, contract/Compose validity, operational instructions. This is infrastructure
evidence, not domain evidence — **no real login, membership, ownership, building
lifecycle, subscription, or back-office operation exists yet**.

Reusable as-is: service boundaries and separate per-service databases; shared web
security/error/correlation conventions; Flyway; the versioned Kafka event envelope;
local infrastructure (`infra/docker/`) and CI (`.github/workflows/platform.yml`). The
current gateway has only two exact platform metadata routes; CORS permits GET/OPTIONS
only. All business/auth/platform routes and allowed methods/headers need explicit,
new, scoped changes — nothing here presumes the old BOS-002 direct-building-creation
model.

Not started at all (genuinely greenfield within this brownfield repo): platform-role
tables/authorization (`PlatformRole`, `PlatformUserRole`), `BuildingApplication` /
`BuildingOnboarding` / `AssistedOnboardingSession` / `SupportAccessGrant` entities and
their state machines, `subscription-service` and its entities
(`SubscriptionPlan`, `BuildingSubscription`, `Entitlement`), and
`buildingos_backoffice_web` (no such app or directory exists in this repo).

## Flutter source observations (user_app, unchanged since BOS-002)

| Source | Observed behavior | Consequence for BOS-010 |
|---|---|---|
| `user_app/lib/features/authentication/data/repo_impl/auth_http_impl.dart` | Email login and legacy user/token payload | Adapt provider identity exchange and BRD API envelope; do not assume existing flow matches phone OTP |
| `user_app/lib/features/authentication/data/repo_impl/auth_cache_impl.dart` | Serializes UserInfo including token fields through PreferenceCache | Review session lifecycle and storage error propagation |
| `user_app/lib/features/authentication/presentation/bindings/login_binding.dart` | AuthRepository is wired to AuthCacheImpl/PreferenceCache | Existing dependency bindings can be reused |
| `user_app/lib/core/data/cache/preference/shared_preference.dart` | `SharedPreference` wraps FlutterSecureStorage; write errors return false | Do not misclassify as plaintext storage; verify callers react to storage failures |
| `user_app/lib/core/data/cache/client/preference_cache.dart` | Calls secure wrapper; ignores its boolean write result; `cache.forever` does not set session expiry | Token/session expiry and write-failure behavior need explicit tests |
| `user_app/lib/core/data/http/client/api_client.dart` | Debug `PrettyDioLogger` enables headers/request/response body logging | Redact/suppress credentials, OTPs and tokens, including debug builds |
| Same `api_client.dart` | Failed refresh clears callbacks without completing waiting completers | Concurrent-request hang risk; reproduce and fix with concurrent refresh-failure tests |
| Same `api_client.dart` | Refresh expects `access_token`/`refresh_token` at response root | Adapt to agreed session/API contract and bound retry/replay behavior |
| `user_app/lib/services/utilities/secure_storage_service.dart` | Separate FlutterSecureStorage wrapper returns typed results | Evaluate reuse/consolidation during technical design |

Preserve GetX/Clean Architecture, feature generator, routes, localization, themes and
spacing conventions. Do not migrate architecture or rename the `user_app` package.
No back-office web client exists yet, so it has no legacy conventions to preserve —
technical design chooses its stack fresh (BRD §149.1 suggests but does not mandate
Flutter Web).

## Baseline limitations

`user_app/.fvmrc` pins Flutter 3.44.4; the BOS-001 record's SDK/engine compile-mismatch
note is unreverified. Run the existing Flutter baseline check before mobile
implementation and distinguish toolchain failures from application regressions.
No provider SDK, SMS service, real account, secret, live Google configuration, or
subscription/payment provider was used or contacted. No code or dependencies changed
during this intake/context pass.

## Freshness

Reuses the BOS-002 context fingerprints for the BRD, BOS-001 closure/review, shared
security/routes/contracts, and the Flutter pin/pubspec/source files above (unchanged).
This baseline additionally records that platform-role, building-lifecycle,
subscription, and back-office code does not exist anywhere in the repository as of
2026-09-22 — there is nothing to reconcile for those areas, only the BRD to build
against.
