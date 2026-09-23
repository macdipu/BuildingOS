# TASK-004 — Architecture conformance cleanup (backend + Flutter)

## Status: IMPLEMENTED (2026-09-23) incl. Revisions 1-2; REVIEW next

Runs before TASK-003 (on hold). Source: architecture audit 2026-09-23 and operator rule:
every feature follows **Clean Architecture + feature-first + use case + repository
pattern, SOLID, microservices in one monorepo**. Target layouts are BRD §10–12
(Spring) and §36 (Flutter). Behavior-preserving refactor: no API contract, DB schema,
or UX change.

## Operator decisions (2026-09-23)

- Backend repository interfaces live in `domain/repository` (BRD §11).
- Flutter: full BRD §36 layout.
- Operator's in-progress cache split (`SecurePreference`/`PlainPreference`,
  `base_preference.dart`) is folded into this task and committed with it.

## Backend (identity-service, building-service)

Per feature: `domain/{model,repository}`, `application/{port/in,port/out,usecase,command}`,
`infrastructure/{persistence,security,otp?,config,seed}`, `presentation/{rest,request,response}`.

1. `identity.auth`
   - `User`, `OtpChallenge`, `PlatformRole` → `domain/model`.
   - `UserRepository`, `OtpChallengeRepository` → `domain/repository`; JDBC adapters stay
     in `infrastructure/persistence`.
   - In-ports renamed `StartOtpChallengeUseCase`, `VerifyOtpChallengeUseCase`; inputs as
     `application/command/{StartOtpChallengeCommand,VerifyOtpChallengeCommand}`.
   - Request/response records extracted from `AuthController` to
     `presentation/{request,response}`.
   - `JwkSetController` → `presentation/rest`, calling new `GetPublicSigningKeysUseCase`
     (in-port) backed by out-port `SigningKeyProvider` (implemented by the local issuer).
   - `SuperAdminSeeder` stops using `JdbcTemplate`: new `SeedSuperAdminUseCase` uses
     `UserRepository.findOrCreateByPhone` + new `UserRepository.grantPlatformRole`;
     the local/test `ApplicationRunner` in `infrastructure/seed` only invokes the use case.
   - `AuthConfiguration` → `infrastructure/config` (wiring only).
2. `platform` feature in both services renamed `servicemeta` (avoids clash with the
   `com.buildingos.platform.web` shared kernel): `GetServiceMetadata` →
   `GetServiceMetadataUseCase`, `ServiceMetadata` → `domain/model`.
3. **Enforcement:** ArchUnit (test scope) `ArchitectureTest` in identity-service and
   building-service:
   - `domain` depends only on `java..` (no Spring, JDBC, Jakarta, Nimbus).
   - `application` does not depend on `infrastructure`/`presentation` or Spring/JDBC.
   - `presentation` depends on `application.port.in`/`command` + `platform.web`, never on
     `infrastructure` or `domain.repository`.
   - No layer-first packages at service root (`controllers`, `services`, `repositories`).
   - **Microservice isolation:** `com.buildingos.identity..` and `com.buildingos.building..`
     never depend on each other; only `com.buildingos.platform.web` is shared.
4. `api-gateway` unchanged (thin routing module); `platform-web` unchanged.

## Flutter (`user_app`)

Target (BRD §36): `lib/app/{routes,bindings,theme,config,shell,app.dart}`,
`lib/core/{network,database,auth,errors,utils,widgets,usecases,services,entities}`,
`lib/features/<f>/{presentation/{pages,widgets,controllers,bindings},domain/{entities,repositories,usecases},data/{models,datasources,repositories,mappers}}`.

1. **Moves:** `core/data/http` → `core/network`; `core/data/cache` → `core/database`;
   `core/domain/error` → `core/errors`; `core/domain/usecase` → `core/usecases`;
   `core/domain/models` → `core/entities`; `core/domain/extensions`, `core/presentation/{utils,hooks}`
   → `core/utils`; `core/presentation/widgets` → `core/widgets`;
   `core/presentation/theme` → `app/theme`; `res/routes` → `app/routes`;
   `services/*` → `core/services`; `app/flavours` → `app/config`; `app/views/app.dart` →
   `app/app.dart`. `res/drawable`, `res/strings` stay (resources, not layers).
2. **Authentication feature:** `domain/model` → `domain/entities` (**pure: no JSON**);
   JSON moves to `data/models` + `data/mappers`; `AuthHttpImpl` → `data/datasources/auth_remote_datasource.dart`,
   `AuthCacheImpl` → `data/datasources/auth_local_datasource.dart`, one
   `data/repositories/auth_repository_impl.dart`; `use_case` → `usecases`;
   `login/screens` → `pages`; `controller` → `controllers`.
3. **Delete dead code:** password login (`DoLoginUseCase`, `AuthRepository.login`,
   `Password`, `AuthLoginReq`, `emailLoginUrl`), `reset_pin/` flow + routes + strings,
   unimplemented gmail/facebook/registration URLs, unused `dashboard_api_urls.dart`.
4. **Layer leak:** `ApiClient` no longer navigates on 401; it signals session expiry via
   `core/auth` (`SessionExpiryNotifier` stream); the app layer listens and routes to login.
5. **Enforcement:** `test/architecture/layer_rules_test.dart` scans imports: feature
   `domain` imports no `flutter`, `get`, `dio`, `data/`, `presentation/`; `data` imports
   no `presentation/`; features never import another feature's internals.
6. `tool/feature_generator/templates.dart` generates the same layout.

## Revision 1 — operator target structure (2026-09-23, during IMPLEMENTATION)

Operator supplied the canonical monorepo tree; backend layout follows it:

- **application:** one package per use case — `application/<usecase>/{<X>Command|<X>Query,
  <X>Result, <X>UseCase (interface), <X>Service (impl)}` (e.g. `startotp`, `verifyotp`,
  `seedsuperadmin`, `getpublickeys`, `getservicemetadata`). `application/port/out` holds
  the remaining out-ports (`OtpCodeVerifier`, `TokenIssuer`, `SigningKeyProvider`).
  Replaces `port/in` + `usecase/` + `command/`.
- **domain:** `model/`, `repository/` (+ `service/`, `event/`, `exception/` created when
  first needed — no empty packages).
- **persistence:** keep **JDBC** (operator decision) in the same shape:
  `infrastructure/persistence/{repository/Jdbc<X>RepositoryAdapter, mapper/<X>RowMapper}`;
  no `entity/` package, no JPA dependency, SQL unchanged.
- **presentation:** `presentation/rest/{<X>Controller, request/, response/, mapper/<Feature>ApiMapper}`.
- **platform-web:** `response/{ApiEnvelope,ApiError}`, `exception/PlatformErrorHandler`,
  `correlation/CorrelationFilter`, `security/SecuritySettings`, `config/PlatformWebConfiguration`.
- **api-gateway:** `routing/config/GatewayRouteConfig` (from `GatewayRoutes`); `routing/filter`,
  `security/*`, `common/config` created when needed.
- ArchUnit rules updated: presentation may depend only on `application.<usecase>`
  interfaces/commands/results + `platform.web`; never `application.port.out`,
  `infrastructure`, or `domain.repository`.

## Revision 2 — service rename (operator, 2026-09-23)

`identity-service` → **`account-service`** everywhere: Maven module/artifactId, Java
package `com.buildingos.identity` → `com.buildingos.account` (inner `auth` feature
kept), `IdentityApplication` → `AccountApplication`, database `identity_db` →
`account_db`, role `identity_app` → `account_app`, env `IDENTITY_DB_PASSWORD` →
`ACCOUNT_DB_PASSWORD`, `IDENTITY_SERVICE_URL` → `ACCOUNT_SERVICE_URL`, compose,
postgres init, `.env.example`, scripts, living docs (`docs/*`, TASK-003). Local dev must
run `docker compose -f infra/docker/compose.yaml down -v` once (wipes local dev data).
Historical records (TASK-001/002, REVIEW-QA, RELEASE, run history) keep the old name.

## Acceptance

1. `mvn -B verify` green incl. new ArchUnit tests; behavior identical (51+ tests).
2. `flutter analyze` clean; all Flutter tests pass incl. new layer-rules test.
3. Live login smoke on emulator (start → verify → app shell) still works.
4. Generator test passes and produces BRD layout.
5. No API, DB migration, or UX change.

## Implementation evidence (2026-09-23)

| Check | Result |
|---|---|
| `mvn -B clean verify` (backend) | BUILD SUCCESS, 66 tests (platform-web 5, account-service 33, building-service 17, api-gateway 11), 0 fail |
| ArchUnit | 7 rules per service, all pass (layering, no `*Service` from presentation, no layer-first root, isolation, only platform-web shared) |
| `fvm flutter analyze` | No issues found |
| Flutter tests | 28 passed (incl. 5 layer-rule tests, generator suite) |
| `scripts/check-contracts.py`, `compose config`, `sh -n verify-platform.sh` | pass |
| Live smoke (emulator API 37, fresh `account_db`) | start 200 → wrong code 401 `OTP_INVALID_CODE` → `000000` 200 → `/app_shell`, stack cleared; session only in encrypted `FlutterSecureStorage.xml`; JWKS public-only |

Visible changes (from Revision 2): gateway path `/api/v1/platform/identity` → `/api/v1/platform/account`;
`/internal/platform/info` service name `account-service`. Flyway `V1`/`V2` left untouched
(immutable; `V1` still seeds metadata row text `identity-service`, not read by `/info`).

Security fixes surfaced during refactor: operator cache split defaulted `secure: false`;
token/user-info reads/writes now pass `secure: true` (verified on device).

Follow-ups: 15 `core/widgets|services` files import `app/` (theme, global navigator) —
allowed by rules, candidate to move into core; README Docker section references missing
`user_app/scripts/start.sh`; local dev must rotate the local account DB password
(value was echoed into a session transcript).
