# BOS-010 TASK-005 — Implementation, review and QA evidence

Run `RUN-8ED649CAEBFA46CD8E6EC1EB2BB9FCBF`. Design: [tasks/TASK-005.md](tasks/TASK-005.md) (operator approved as written, 2026-09-23).

## Implementation (agent, 2026-09-23)

| Part | Files |
|---|---|
| Async locale toggle | `lib/core/controllers/locale_controller.dart` (sync flip + `Get.updateLocale`, background `setLocale`, `debugPrint` on failure; repository injectable) |
| SVG precache | `lib/core/widgets/images/app_svg.dart` (`SvgPicture.asset`, `AppSvg.precache` via `SvgAssetLoader.loadBytes` → `svg.cache`); `lib/app/app.dart` precaches `splash.svg` in `initState` |
| Login polish | `pages/login_screen.dart`: bottom duplicate logo removed, 96 dp `AppSvg` logo, `loginUpperText` heading, keyboard resize, full-width button |
| OTP polish | `pages/login_otp_verify_screen.dart`: `SafeArea` inside `Scaffold`, translated strings, `TextButton` Change Number, 16/24 dp spacing; `widgets/intro_header.dart` const, theme from context |
| Strings | `res/strings/string_enum.dart`: `otpVerification`, `otpSentTo` (`@phone`), `changeNumber` |
| Tests | `test/core/controllers/locale_controller_test.dart` (UI flips before save completes; failed save keeps locale, no throw); `test/core/widgets/app_svg_test.dart` (precache stores entry under `SvgPicture.asset` key) |

| Check | Result |
|---|---|
| `fvm flutter analyze` | No issues found |
| `fvm flutter test` | 31 passed (28 existing + 3 new) |

## Device check (emulator-5554, API 37, debug, local gateway via `adb reverse`)

| # | Step | Observed | Result |
|---|---|---|---|
| 1 | Launch (en) | One logo, "Login to continue", field, full-width Next ([01](qa/task-005/01-login-en.png)) | PASS |
| 2 | Toggle → বাংলা | All login strings switch immediately ([02](qa/task-005/02-login-bn.png)) | PASS |
| 3 | Keyboard open | Next stays visible above keyboard ([03](qa/task-005/03-login-keyboard.png)) | PASS |
| 4 | Next → OTP (bn) | No black status band; title/intro/Change Number/Verify in Bangla, wraps cleanly ([04](qa/task-005/04-otp-bn.png)) | PASS |
| 5 | `111111` | Error snackbar, stays ([05](qa/task-005/05-otp-wrong-bn.png)) | PASS |
| 6 | `000000` | App shell ([06](qa/task-005/06-shell.png)) | PASS |
| 7 | Force-stop + cold launch | Login in Bangla (persisted) ([07](qa/task-005/07-cold-restart-bn.png)) | PASS |
| 8 | Toggle → English | Immediate ([08](qa/task-005/08-toggle-back-en.png)) | PASS |

Out of scope, still English in Bangla mode (follow-up candidates): `OtpResendButton` "Resend in mm:ss"
(`core/widgets/buttons/otp_resend_timer.dart`), OTP error snackbar text, app-shell tab labels.
Logo placeholder absence is not visually distinguishable on a fast emulator; covered by the cache-key test.

## Code review round 1 (agent, 2026-09-23) — CHANGES_REQUIRED → fixed

1. Fixed: rapid double toggle ran two concurrent `setLocale` calls; out-of-order completion could
   persist the older locale. `LocaleController` now chains saves (`_pendingSave.then(...)`), so the
   last toggle wins on disk. New test `rapid toggles persist in order so the last choice wins`
   (fails on the unchained version, passes with the fix). Suite: 32 passed; analyze clean.

Passed: AppSvg cache key vs `SvgPicture.asset`; SafeArea/keyboard/layout; `IntroHeader`;
`trParams` `@phone`; style/layer rules; tests non-vacuous.

Harness: stages are forward-only, so the fix required `reopen` (scope revision 1), which clears
results and the technical approval; design unchanged.

## QA (agent, 2026-09-23, after review fix)

| Acceptance | Check | Result |
|---|---|---|
| 1 Toggle immediate, persisted, failure-safe | 3 `LocaleController` tests; device: en → 3 rapid taps → বাংলা → cold restart → বাংলা ([09](qa/task-005/09-before-rapid.png), [10](qa/task-005/10-after-rapid.png), [11](qa/task-005/11-rapid-cold-restart.png)) | PASS |
| 2 Logo from `svg.cache` | `app_svg_test.dart` | PASS |
| 3 Login/OTP flow unchanged | device steps 1–6 above | PASS |
| 4 en/bn render, keyboard | device steps 2–4 | PASS |
| 5 analyze/tests/layer rules | `fvm flutter analyze` no issues; `fvm flutter test` 32 passed | PASS |
