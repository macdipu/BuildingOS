# TASK-005 — user_app: async locale toggle, login SVG precache, login/OTP layout polish

## Status: TECHNICAL (awaiting operator approval)

Category: Mobile (`mobile-agent`). Scope: `user_app/` only; no backend, API, or auth-flow change.
Source: operator prompt 2026-09-23 (pasted change description + "also change login and otp ui"),
operator choice "Polish current layout" (same content and flow; agent proposes, operator reviews on device).

## 1. Async locale toggle

`LocaleController.toggleLocale` today awaits `AppSettingsRepository.setLocale` before updating the UI.

- New flow: flip `currentLangCode`, call `Get.updateLocale` immediately, persist with `setLocale`
  in the background (`unawaited`); a save failure is caught and logged with `debugPrint`.
- No repository rename: `setLocale` only writes; nothing reads disk during the toggle.
- `LoginScreenController.toggleLocale` keeps delegating.

## 2. Login SVG precache

No per-locale logo exists. The login screen renders `Resources.drawable.splashImage`
(`assets/svg/splash.svg`) via `CRoundImage` → `AnyImageView`.

Finding (any_image_view 2.1.0, locked): `AnyImageView` reads the asset with
`rootBundle.loadString` inside a `FutureBuilder` and renders `SvgPicture.string`. A
flutter_svg asset-keyed precache is never hit (the cache key is the string, not the asset), and
the `FutureBuilder` shows its loading widget for at least one frame regardless of any cache.

Design: the login logo renders with `SvgPicture.asset` (flutter_svg, already a direct
dependency) through a small `core/widgets/images/app_svg.dart`, and `MyApp.initState` precaches
it with `SvgAssetLoader(path)` into `svg.cache` using the same cache key the widget computes
(default `SvgTheme`, root bundle, no colour mapper — verified in flutter_svg 2.3.0
`loaders.dart`). Other `CRoundImage` uses are unchanged.

## 3. Login layout polish (same content and flow)

- Remove the duplicated full-width logo drawn behind the form (`_bottomImage` Stack); keep one
  centered brand logo (larger, 96 dp).
- Add the existing `TextEnum.loginUpperText` ("Login to continue") heading under the logo.
- Let the scaffold resize with the keyboard (drop `resizeToAvoidBottomInset: false`) so the
  Next button stays reachable.
- Next button full width via layout, not `MediaQuery` width math; locale toggle unchanged.

## 4. OTP layout polish (same content and flow)

- `SafeArea` moves inside `Scaffold` (removes the black status-bar band seen in QA shots).
- Hardcoded English strings become translated `TextEnum` keys: `otpVerification`,
  `otpSentTo` (`@phone` param), `changeNumber`, and existing `verify`.
  Proposed Bangla: "OTP যাচাইকরণ", "@phone নম্বরে একটি যাচাইকরণ কোড পাঠানো হয়েছে",
  "নম্বর পরিবর্তন করুন".
- "Change Number" becomes a `TextButton` (48 dp tap target) aligned left.
- Consistent 16 dp horizontal padding and 24 dp section spacing on both screens.
- `IntroHeader`: read theme from `build` context (drop `Theme.of(Get.context!)` field), const constructor.

## Acceptance

1. Toggle switches language on the same frame; persisted value survives app restart;
   a failing save does not revert or block the UI (unit test with failing repository).
2. Login logo shows no loading placeholder on cold launch (bytes served from `svg.cache`;
   widget test asserts the cache entry exists after precache).
3. Login/OTP flow unchanged: start → OTP → wrong code error → `000000` → app shell.
4. Both screens render in English and Bangla without overflow; keyboard does not cover the
   primary button.
5. `flutter analyze` clean; all tests pass; layer-rules test still passes.
