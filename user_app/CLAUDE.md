# Clean Architecture GetX — Project Guide

Canonical layout and layer rules: [`../docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md) ("Flutter app").

## Stack
Flutter + GetX state management + Clean Architecture (data/domain/presentation per feature)

## Commands
```bash
flutter pub get
flutter analyze
flutter test
flutter run
flutter build apk --release
dart generate_feature.dart <snake_case_name>
```

## Environment Setup
Copy `.env/example.json` to `.env/dev.json` (or `staging.json` / `prod.json`) and fill in values.
Only `example.json` is committed — the rest are gitignored.
Config is compiled in via `--dart-define-from-file=.env/dev.json`, read in `AppConfig`
through `String.fromEnvironment`/`bool.fromEnvironment`. Never bundle env files as Flutter assets.
Run/build with the matching file, e.g. `flutter run --dart-define-from-file=.env/dev.json`.

## Feature Generation
```bash
dart generate_feature.dart user_profile
```
After generating:
1. Update entity fields in `domain/entities/`
2. Update response DTO in `data/models/` and entity mappings in `data/mappers/`
3. Set the absolute `_endpoint` URL in `data/datasources/xxx_remote_datasource.dart` (search TODO)
4. Import `presentation/xxx_pages.dart` and spread `...XxxPages.routes` in `lib/app/routes/app_pages.dart`
5. Navigate using `XxxPages.routeName`; a central alias in `lib/app/routes/app_routes.dart` is optional

The generator detects the package name, formats output, and requires `--force`
to overwrite an existing feature. Use `--dry-run` to preview paths. Refresh and
Retry bypass the cache. Run `flutter test test/tool/feature_generator_test.dart`
after changing generator templates in `tool/feature_generator/templates.dart`.

## Architecture Rules (ENFORCED)

### Layer boundaries — never cross these:
- `core/` NEVER imports from `features/`
- `core/network/` and `core/database/` NEVER import `app/` (or `features/`)
- `features/xxx/domain/` NEVER imports `flutter`, `get`, `dio`, `data/` or `presentation/`
- `features/xxx/data/` NEVER imports `presentation/`
- Features NEVER import another feature's internals

### DI rules:
- ALWAYS use `Get.find<T>()` in widget/controller field initializers (binding provides it)
- NEVER use `Get.put(Controller())` inside a widget State class
- Register with `Get.lazyPut(..., fenix: true)` in Bindings
- Global singletons (ApiClient, PreferenceCache, etc.) registered in `lib/app/config/app_flavour.dart`

### State management:
- Async operations → use `doAction<T>()` from `BaseController`
- Initial data fetching → override `onInit()`, NOT constructor
- List updates → `RxList.assignAll()`, not `RxList.value = ...`
- Dispose TextEditingControllers in `onClose()`

### Security:
- Auth tokens stored via `flutter_secure_storage` (NOT SharedPreferences)
- Secrets via `--dart-define-from-file=.env` (NOT bundled assets)
- Never hardcode credentials, tokens, or API keys in source
- `devAutoFill` test helpers MUST be wrapped in `assert(() { ... }())`

### Code style:
- Entities in `domain/entities/` are PURE Dart — no fromJson/toJson
- DTOs in `data/models/` handle all JSON serialization; `data/mappers/` convert DTO → entity
- No `!` (bang) on nullable unless provably non-null at that point
- No `print()` — use `debugPrint()` or Logger
- No comments explaining WHAT — only WHY (non-obvious constraints/workarounds)

## Project Structure
```
lib/
├── app/
│   ├── app.dart     — MyApp root widget
│   ├── config/      — AppConfig (compile-time env), bootstrap DI (app_flavour.dart)
│   ├── routes/      — AppPages, AppRoutes, global navigator
│   ├── theme/       — color schemes, text theme, dimensions, theme extensions
│   └── shell/       — Bottom nav shell + binding
├── core/
│   ├── network/     — HTTP client, API URLs
│   ├── database/    — cache client, secure/plain preferences
│   ├── auth/        — JWT, session-expiry notifier
│   ├── errors/      — failures, exceptions
│   ├── usecases/    — base UseCase
│   ├── entities/    — shared value objects (e.g. PhoneNumber)
│   ├── controllers/ — BaseController, theme/locale controllers
│   ├── services/    — navigation, push notifications, platform utilities
│   ├── utils/       — extensions, hooks, helpers
│   └── widgets/     — shared widgets
├── features/
│   └── xxx/
│       ├── data/    — models (DTO), datasources (remote, local), repositories (impl), mappers
│       ├── domain/  — entities, repositories (interface), usecases
│       └── presentation/ — pages, widgets, controllers, bindings, xxx_pages.dart
└── res/             — strings, drawables
```

## Known TODOs (do before production)
- [ ] Enable Firebase (`lib/app/config/app_flavour.dart` TODO comment)
- [ ] Add `google-services.json` / `GoogleService-Info.plist`
- [ ] Replace `flutter_secure_storage` stub calls with actual secure token storage
- [ ] Write unit tests for all UseCases and repository implementations
- [ ] Add integration tests for critical flows (login, navigation)
- [ ] Configure CI/CD pipeline (GitHub Actions or Fastlane)
- [ ] Add SSL/TLS certificate pinning to `ApiClient`
- [ ] Set up crash reporting (Firebase Crashlytics)
