# BuildingOS scoped baseline
Observed 2026-09-22. Scope: platform startup and client integration seams, not a whole-app audit.
Graph: Volumes-dipu_1tb-projects-chowdhuryelab-BuildingOS-user_app (fast index).
Confidence: source-backed observations below; runtime application behavior is unverified.

## Current repository
- user_app is an existing Flutter/GetX Clean Architecture template, package customer.
- Flutter pin: 3.44.4 in user_app/.fvmrc. Existing services, theme, HTTP client, routes, localization and generator are reusable.
- AppShell currently renders Home / Explore / Account placeholders.
- AppConfig uses compile-time configuration and defaults to api.example.com.
- AuthHttpImpl calls an email-login endpoint and expects messageCode/response; this differs from the BRD phone/Google flow and success/data/meta/traceId envelope.
- AuthCacheImpl persists UserInfo through PreferenceCache; token fields and refresh behavior need a targeted security review before real authentication.
- pubspec.yaml has SharedPreferences/secure storage.
- backend/, contracts/, root .github/workflows/ do not exist.
- Framework detection during kit adoption did not find the nested Flutter module.
- Existing BRD was staged before this session; preserve it unchanged.

## Environment evidence
Read-only commands succeeded:
- java -version: OpenJDK 17.0.16.
- mvn -version: Maven 3.9.12 using Java 17.
- docker compose version: 5.1.2.
- fvm and flutter are on PATH; project Flutter execution is not yet verified.
Docker daemon availability, container pulls, Maven artifact resolution and platform builds are unverified.
No Flutter analysis/tests or backend tests were run during this design baseline.
The prior installation session passed 49 kit tests; that is not application QA.

**Flutter baseline failures (recorded 2026-09-22, not fixed — out of BOS-001 scope):**
- `fvm flutter analyze --no-pub` (user_app): 144 pre-existing info-level lints (prefer_final_locals, prefer_const_constructors, unawaited_futures, avoid_redundant_argument_values). No errors/warnings; all info-severity template debt.
- `fvm flutter test --no-pub` (user_app): fails to compile. Pinned Flutter 3.44.0's own `packages/flutter/lib/src/gestures/binding.dart` references `ui.HitTestResponse`/`ui.HitTestRequest`/`PlatformDispatcher.onHitTest`, which don't exist in the resolved Dart/engine SDK — an internal Flutter-SDK/engine version mismatch in the pinned toolchain, not a defect in user_app's own code. Blocks every test in the suite from loading.
- These are pre-existing conditions surfaced by first-ever `scripts/verify-flutter.sh` run, not introduced by BOS-001. Fixing the Flutter/Dart SDK pin mismatch is a separate, unrelated task.

## Reuse and impact
Keep user_app in place; do not rename package or move directories as part of foundation.
Keep UI → controller → use case → repository boundaries, GetX bindings, existing semantic theme and spacing tokens.
Use generator for new Flutter features. Do not use generic preference cache for financial mutations.
Backend foundation is new and can be tested independently of the client.

## Refresh triggers
Refresh these findings when the BRD, app config/shell/auth sources, pubspec, Flutter pin, or local toolchain changes.
Runtime context command records SHA-256 fingerprints of the reviewed files. No claim of existing backend behavior is made.

## Risks and open dependencies
- BRD §17 puts initial invoice generation in Finance, while §§8.3,9,91 assign rent invoices to Rental. Resolve before BOS-003.
- Real phone/Google login requires provider, credential and bootstrap decisions before BOS-002.
- Permission examples are not a complete authorization matrix.
- SDK/container/dependency access may require environment approval.
- Harness command allowlist currently covers kit tests only. Register exact scoped application commands during governed foundation setup; configuration changes invalidate existing pins, so reconcile run state explicitly.
