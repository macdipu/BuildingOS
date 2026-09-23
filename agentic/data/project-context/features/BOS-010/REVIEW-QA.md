# BOS-010 identity slice — continuation checkpoint (2026-09-23)

Scope: TASK-001 backend and TASK-002 Flutter phone+OTP login. Starting revision:
`39842eb` (TASK-002 is committed; the previous handoff's uncommitted claim is stale).
Production authentication and the later platform features remain out of scope.

## Changes and evidence

- Added nine loopback HTTP tests in
  `user_app/test/features/authentication/data/otp_repository_http_test.dart`.
  They exercise real Dio/ApiClient, AuthHttpImpl, AuthCacheImpl, and UserInfo
  serialization. Only the server and device-storage boundary are substituted.
  Covered: normalized/public start request and envelope; verify request and
  persisted roles/token; token installed before return; all six 401 OTP reasons
  retained without refreshing or overwriting an existing session; 500 cannot
  create a session. These are contract-fixture tests, not a live-backend E2E run.
- `scripts/verify-flutter.sh` now executes both analysis and tests even if analysis
  fails. It still returns failure if either check fails; no lint policy was relaxed.
- `fvm flutter pub get` regenerated stale package configuration pointing at Flutter
  3.44.0 while `.fvmrc` selects 3.44.4. This resolved missing dart:ui types during
  test compilation. The resulting lockfile aligns intl, matcher, test_api, and
  vector_math with the selected SDK; no new dependency was introduced.

## Verification

| Check | Result |
|---|---|
| Governed `sh scripts/verify-flutter.sh` after package refresh | Exit 1: analyze=1, test=0 |
| Flutter tests executed by that script | **22 passed**, including 9 new HTTP tests |
| Flutter analysis executed by that script | 146 informational findings; not a clean analysis gate |
| `fvm dart analyze test/features/authentication/data/otp_repository_http_test.dart` | Exit 0, no issues |
| `git diff --check` | Exit 0 |
| Governed `mvn -B -f backend/pom.xml verify` | Incomplete: Docker unavailable; identity container suites errored and downstream reactor modules skipped |
| Backend retry after `orbctl start` | Not executed: harness rejected it after task time budget expired |

Local command capture: `/private/tmp/bos010-flutter-check.json` (ephemeral).
The harness run store also records executed tools and task timing. Historical
51/51 backend evidence remains historical; this session does not reconfirm it.
OrbStack was started for verification and left running.

## Review observations and outstanding work

Source inspection confirmed endpoint/body/envelope agreement, the six rejection
codes, cache serialization of platform roles, public OTP calls, and the existing
post-login destination. No runtime authentication behavior was changed here.
The prior claim that Flutter analysis was clean is inaccurate: info diagnostics
produce a nonzero exit under the registered strict command. The new test file
itself analyzes clean; broader lint cleanup/disposition remains outstanding.

Live device login/navigation/resend, real device-storage persistence, and a
Flutter-to-running-backend flow were not exercised. Existing refresh/concurrency
and debug-log redaction debt remains outside this bounded task, as documented in
TASK-002. This checkpoint grants no review, QA, UAT, or release approval.

Run `RUN-D10A3F4277E7424F938E3BC2C46E9A2D` remains IMPLEMENTATION/BLOCKED.
The 900-second task budget expired twice during setup/verification; the original
implementation plus these attempts exhaust `max_agent_retries: 2`. Both expired
tasks were closed with `task-fail`; no active worker remains. Do not alter run
JSON, fabricate approval, or restart this work as another run to evade the limit.
Operator-authorized harness recovery is required before further governed work.

Next bounded step after recovery: refresh the changed registered context and add
this checkpoint/lockfile to scope; rerun backend verification with Docker available;
resolve the strict analysis gate and obtain mobile flow evidence; submit updated
IMPLEMENTATION evidence, then proceed through REVIEW and QA. TASK-003 still needs
an operator-configured production issuer/OTP provider decision.

## Recovery continuation (2026-09-23, Claude)

Operator (macdipu) explicitly authorized a budget extension in-session. Stale
task marker recovered (no worker process present), then `adjust-budget` applied
`max_agent_retries: 4`, `max_task_seconds: 1800`; attempts/approvals preserved.
New IMPLEMENTATION task `1e6bcf723e864133a09793672c4abaf7` started.

| Check | Result |
|---|---|
| `mvn -B verify` (backend, Docker via OrbStack) | **Exit 0; 51 tests, 0 failures/errors/skips** (fresh reports) |
| `flutter analyze` (user_app) | **No issues found** (was 145 infos) |
| Flutter tests (Dart MCP runner) | **22 passed** |
| `git diff --check` | Exit 0 |

Lint cleanup: `dart fix` for `prefer_final_locals`, `avoid_redundant_argument_values`,
`prefer_const_constructors` (135 fixes, 29 files); manual `unawaited()` on
fire-and-forget GetX navigation and `await Get.updateLocale`. Two `dart fix`
regressions corrected by hand:
- `_Duration` fields became illegal `const` instance fields → restored `final`
  with `const Duration(...)` values.
- `PrettyDioLogger(enabled: kDebugMode)` was removed as "redundant" because the
  analyzer evaluates `kDebugMode` as `true`, but pretty_dio_logger 1.4.0 defaults
  `enabled: true` — removal would log OTP/token bodies in release builds.
  Restored with a targeted `// ignore: avoid_redundant_argument_values`.

Observed, not changed (out of scope): `reset_pin_screen.dart` navigates to the
success route before form validation.

Still outstanding: live device login/navigation/resend and Flutter→running-backend
flow evidence (no device session this task). No REVIEW/QA/UAT/release approval granted.

## REVIEW (2026-09-23, code-review-agent, scope rev 3)

Scope reopened (rev 3) to register the lint-gate files; operator re-approved the
technical gate. Reviewed backend commit `c92e56c` (TASK-001), Flutter commit
`39842eb` (TASK-002), and the working-tree lint diff. Verdict: **READY — no
blocking defects**. Agent review is not human approval.

| Sev | Location | Finding | Disposition |
|---|---|---|---|
| ~~Medium~~ Withdrawn (QA) | `user_app/.../auth_cache_impl.dart:48` | Claimed token persisted in plaintext SharedPreferences | **False positive**: `core/data/cache/preference/shared_preference.dart:5` wraps `FlutterSecureStorage`; device shows `customer_info` only in encrypted `FlutterSecureStorage.xml` |
| Low | `identity/.../domain/OtpChallenge.java:13` | `now.isAfter(expiresAt)` accepts the exact expiry instant | Follow-up: `!now.isBefore(expiresAt)` |
| Low | `user_app/.../api_client.dart:36` | Debug builds log OTP/phone request bodies | Accepted: `enabled: kDebugMode`, release silent |
| Info | `DevelopmentOtpCodeVerifier.java:19` | Non-constant-time compare | Not a defect: local/test-only, fixed code is public |
| Info | `AuthController.java:48` | Distinct OTP 401 codes reveal attempt state | Accepted per contract; attemptId is random UUID |

Verified no behavior change from `dart fix`: `firebase_messaging` 16.2.1
`requestPermission` defaults equal the removed arguments; `PrettyDioLogger`
`enabled` kept explicitly (default `true` in 1.4.0).

Test gaps (follow-up, non-blocking): concurrent double-verify, full JWT claim
assertions (`iss`/`aud`/`exp`/`iat`), CORS preflight on public auth paths.
Still required for QA: live device login/resend and Flutter-to-backend flow.

## QA (2026-09-23, automated-qa-agent, live device)

Environment: Android emulator `Pixel_4a` (API 37, debug build, `fvm flutter run`,
`API_BASE_URL=http://localhost:8080/` via `adb reverse`); Postgres (compose);
identity-service `:8081` (`SPRING_PROFILES_ACTIVE=local`, local RSA issuer) and
api-gateway `:8080` from the verified jars. Seed phone `01306999005`.

| # | Step | Expected | Observed | Result |
|---|---|---|---|---|
| 1 | Enter phone, Next | `POST /api/v1/auth/otp/start` 200 → OTP screen | 200, attempt issued, `/login_otp_verify` ([01](qa/01-login.png), [02](qa/02-otp.png)) | PASS |
| 2 | Code `111111`, Verify | 401 `OTP_INVALID_CODE`, stay, error shown | 401 `OTP_INVALID_CODE`; "Incorrect code. Please try again." ([03](qa/03-wrong-code.png)) | PASS |
| 3 | Resend after 60 s timer | New `otp/start`, new attemptId | 200, new attempt `e87b0c10…` | PASS |
| 4 | Code `000000`, Verify | 200 with new attemptId → app shell, back stack cleared | Verify sent new attemptId; 200; stack `[/app_shell]` ([05](qa/05-verified.png)) | PASS |
| 5 | Session persisted | `customer_info` stored | Present in encrypted `FlutterSecureStorage.xml` only | PASS |
| 6 | Cold restart | not specified | Lands on `/login` (`AppPages.initial` static) ([06](qa/06-cold-restart.png)) | GAP (no requirement) |

Gap (needs business decision, not inferred): auto-restore of a persisted, unexpired
session on launch is not specified in TASK-002; the app always starts at login.
Verdict: QA READY for TASK-001/TASK-002 local/test slice. Not UAT/release approval.
