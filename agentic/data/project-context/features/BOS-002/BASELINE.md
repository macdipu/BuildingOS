# BOS-002 scoped baseline

Observed 2026-09-22. Scope: identity/building foundations, gateway security/contracts,
Flutter login/cache/HTTP integration. Source project is this repository. No broad
repository audit or new runtime application check was needed for this planning pass.

## Confirmed reusable foundation

BOS-001 is COMPLETED with explicit local milestone approval. Its release/readiness
and reconciliation evidence record 37 passing backend tests, database bootstrap and
isolation, migration reruns, Kafka record persistence across broker recreation,
contract/Compose validity and operational instructions. This is reused evidence,
not a BOS-002 test pass. No real login, membership or ownership operations exist yet.

Reuse service boundaries and separate databases; shared web security/error/correlation
conventions; Flyway; the versioned event envelope; local infrastructure and CI. The
current gateway has only two exact platform routes and CORS permits GET/OPTIONS;
business/auth routes and allowed methods/headers will need explicit scoped changes.
No cross-service domain/entity dependencies or shared business tables are proposed.

## Flutter source observations

| Source | Observed behavior | BOS-002 consequence |
|---|---|---|
| `user_app/lib/features/authentication/data/repo_impl/auth_http_impl.dart` | Email login and legacy user/token payload | Adapt provider identity exchange and BRD API envelope; do not assume existing flow matches phone/Google |
| `user_app/lib/features/authentication/data/repo_impl/auth_cache_impl.dart` | Serializes UserInfo including token fields through PreferenceCache | Review session lifecycle and storage error propagation |
| `user_app/lib/features/authentication/presentation/bindings/login_binding.dart` | AuthRepository is wired to AuthCacheImpl/PreferenceCache | Existing dependency bindings can be reused |
| `user_app/lib/core/data/cache/preference/shared_preference.dart` | The class named SharedPreference wraps FlutterSecureStorage; write errors return false | Do not misclassify this as plaintext shared-preferences token storage; verify callers react to storage failures |
| `user_app/lib/core/data/cache/client/preference_cache.dart` | Calls secure wrapper; ignores its boolean write result; cache.forever does not set session expiry | Token/session expiry and write-failure behavior require explicit tests |
| `user_app/lib/core/data/http/client/api_client.dart` | Debug PrettyDioLogger enables headers, request bodies and response bodies | Redact/suppress credentials, OTPs and tokens before using real authentication, including debug builds |
| Same ApiClient | Failed refresh clears callbacks without completing waiting completers | Source-indicated concurrent-request hang risk; reproduce and fix with concurrent refresh-failure tests |
| Same ApiClient | Refresh expects access_token/refresh_token at response root | Adapt to agreed session/API contract and bound retry/replay behavior |
| `user_app/lib/services/utilities/secure_storage_service.dart` | Separate FlutterSecureStorage wrapper returns typed results | Evaluate reuse/consolidation during technical design; no new storage package chosen |

Preserve GetX/Clean Architecture, feature generator, routes, localization, themes and
spacing conventions. Do not migrate architecture or rename the user_app package.

## Baseline limitations

Current `user_app/.fvmrc` pins Flutter 3.44.4. The historical BOS-001 record describes
an SDK/engine compile mismatch and also mentions 3.44.0; these version statements
are inconsistent. The current pin is source-confirmed, but the exact installed SDK
cause has not been reverified. Run the existing Flutter baseline check before mobile
implementation and distinguish toolchain failures from application regressions.
Prior analysis recorded 144 info-level lints. No Flutter pass is claimed here.

Source inspection identifies risks, not a reproduced production incident. No provider
SDK, SMS service, real account, secret or live Google configuration was used. No code
or dependencies changed during BOS-002 planning.

## Freshness

Harness context records SHA-256 fingerprints for the BRD, BOS-001 closure/review,
shared security/routes/contracts, Flutter pin/pubspec and the source files above.
Refresh only affected entries when these files or provider decisions change. This
baseline supersedes pre-foundation statements in BOS-001/BASELINE.md that backend,
contracts and CI did not yet exist.
