# BOS-010 TASK-004 — Code review

Run `RUN-512386534E7849B9AEDA5CBCFBD5690D`, stage REVIEW. The run's review result cites `TASK-004.md#code-review-agent-2026-09-23`; that record lives here so the reviewed TASK-004.md scope stays unchanged.

## Code review (agent, 2026-09-23)

Scope: backend commit `0909ba2` + uncommitted `user_app/` Flutter refactor. Verdict: **READY** (agent review, not human approval).

| Check | Result |
|---|---|
| `mvn -B verify` (re-run) | 66 tests, 0 failures/errors (fresh surefire reports) |
| `fvm flutter analyze` / `fvm flutter test` (re-run) | No issues / 28 passed |
| Backend review | Rename complete (db, env, Maven, gateway, scripts); ArchUnit rules non-vacuous; JWKS public-only; seeding local/test only; OTP checks intact |
| Flutter review | OTP start→verify→secure token→`/app_shell`; cold launch → login; 401 → `SessionExpiryNotifier` → app routes to login; pure domain entities; layer-rules test scans real dirs; generator matches §36; dead code removed |

Findings:
1. Fixed: `user_app/android/build/reports/problems/problems-report.html` was staged (build artifact) — unstaged; `/build/` added to `user_app/android/.gitignore`.
2. Accepted (no change): Flyway `V1__platform_metadata.sql` still seeds text `identity-service`; applied migrations are immutable and `/info` does not read the row. Optional follow-up: `V3` data update (needs operator OK; conflicts with "no DB migration" acceptance).

## Automated QA (agent, 2026-09-23)

Revision: HEAD `0909ba2` (backend) + uncommitted `user_app/` working tree. Verdict: **READY** for automated scope.

| Acceptance | Check | Result |
|---|---|---|
| 1 Backend green incl. ArchUnit | `mvn -B verify` | 66 tests, 0 failures/errors |
| 2 Flutter analyze + tests incl. layer rules | `fvm flutter analyze`; `fvm flutter test` | No issues; 28 passed (5 layer-rule tests) |
| 3 Live login smoke | Implementation-stage emulator run (API 37, fresh `account_db`), see TASK-004.md evidence | start 200 → wrong code 401 `OTP_INVALID_CODE` → `000000` 200 → `/app_shell`. No app code changed since; not re-run in QA |
| 4 Generator layout | `test/tool/feature_generator_test.dart` (in 28) | pass |
| 5 No API/DB/UX change | `scripts/check-contracts.py`; `docker compose ... config -q`; `sh -n scripts/verify-platform.sh`; review | pass; only rename-driven gateway path `/api/v1/platform/identity` → `/account` (operator Revision 2) |

Not covered: human live-device QA of the refactored Flutter app (operator), before release.

## Live device smoke (agent, 2026-09-23, QA stage)

Target: Android emulator `emulator-5554` (sdk gphone16k arm64, API 37, virtual), app `com.onkur.customer`
debug build from uncommitted `user_app/` tree on HEAD `0909ba2`:
`fvm flutter run -d emulator-5554 --dart-define=API_BASE_URL=http://localhost:8080/ --dart-define=APP_DEBUG=true --dart-define=DEFAULT_LOCALE=en`
with `adb reverse tcp:8080 tcp:8080`. Backend: Postgres (compose), account-service `:8081`
(`SPRING_PROFILES_ACTIVE=local`) and api-gateway `:8080` from jars built by the QA `mvn verify`.

| # | Step | Observed | Result |
|---|---|---|---|
| 0 | API via gateway | start 200; `111111` → 401 `OTP_INVALID_CODE`; `000000` → 200; JWKS has no private fields; unauth `/api/v1/platform/account` 401 | PASS |
| 1 | Launch | Login screen, no overflow/error overlay ([01](qa/task-004/01-login.png)) | PASS |
| 2 | Phone `01306999005`, Next | OTP screen, "+88 01306999005", resend timer ([02](qa/task-004/02-otp.png)) | PASS |
| 3 | Code `111111`, Verify | Stays; "Incorrect code. Please try again." ([03](qa/task-004/03-wrong-code.png)) | PASS |
| 4 | Code `000000`, Verify | App shell, Home/Explore/Account tabs ([04](qa/task-004/04-verified.png)) | PASS |
| 5 | Back from shell | Exits to launcher (auth stack cleared) | PASS |
| 6 | Storage | Only `FlutterSecureStorage.xml` (3 encrypted entries); no plain `FlutterSharedPreferences.xml` | PASS |
| 7 | Force-stop + cold launch | Login screen (decision: always start at login) ([05](qa/task-004/05-cold-restart.png)) | PASS |

Not checked: 401 session-expiry redirect on device (covered by review of `ApiClient` → `SessionExpiryNotifier` → app listener only; no device trigger available without an expired token).
