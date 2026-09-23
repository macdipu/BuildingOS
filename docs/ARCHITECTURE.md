# BuildingOS architecture standard

Canonical structure for the monorepo (operator-approved 2026-09-23, BOS-010 TASK-004).
Every feature, backend and mobile, follows **Clean Architecture + feature-first +
use case + repository pattern + SOLID**. The backend is **microservices in one monorepo**.
Sources: BRD §10–12 (Spring), §36 (Flutter).

## Rules

1. **Feature-first, layer-second.** No global `controllers/`, `services/`,
   `repositories/`, `entities/` at a service root.
2. **Dependency direction:** `presentation → application → domain`; `infrastructure`
   implements ports and depends inward. `domain` depends on nothing but the JDK.
3. **One use case per business action.** Each gets its own package with its
   command/query, result, use-case interface and service implementation.
4. **Repository interfaces live in `domain/repository`**; adapters in
   `infrastructure/persistence`. Persistence is **JDBC** (no JPA) unless a
   decision says otherwise.
5. **Controllers:** validate transport input → map request to command (`mapper/`) →
   call the use-case *interface* → map result to response. Never call repositories,
   out-ports, services or infrastructure directly.
6. **Microservice isolation:** each service owns its database; services never import
   each other's code or read each other's DB. Only `platform-web` (shared web kernel)
   is shared. Cross-service communication goes through HTTP contracts
   (`contracts/openapi`) or events (`contracts/kafka`).
7. **No empty packages.** Folders such as `domain/service`, `domain/event`,
   `domain/exception` are created when the first class needs them.
8. **Enforced by tests:** ArchUnit `architecture/ArchitectureTest` in each service;
   `test/architecture/layer_rules_test.dart` in `user_app`.

## Monorepo

```text
BuildingOS/
├── backend/                 Maven multi-module (pom.xml)
│   ├── api-gateway/
│   ├── account-service/
│   ├── building-service/
│   └── platform-web/        shared web kernel (library, not a service)
├── user_app/                Flutter mobile app (GetX)
├── contracts/
│   ├── openapi/             HTTP contracts
│   └── kafka/               event envelope schemas
├── infra/docker/            compose.yaml, postgres init
├── scripts/                 verify-platform.sh, verify-flutter.sh, check-contracts.py
└── docs/                    this file, LOCAL_DEVELOPMENT.md
```

## Backend service (example: account-service)

```text
account-service/
├── pom.xml
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/buildingos/account/
    │   │   ├── AccountApplication.java
    │   │   ├── auth/                                  ← feature
    │   │   │   ├── domain/
    │   │   │   │   ├── model/            User, OtpChallenge, PlatformRole
    │   │   │   │   ├── repository/       UserRepository, OtpChallengeRepository
    │   │   │   │   ├── service/          (when needed) domain services
    │   │   │   │   ├── event/            (when needed) domain events
    │   │   │   │   └── exception/        (when needed) domain exceptions
    │   │   │   ├── application/
    │   │   │   │   ├── startotp/         StartOtpCommand, StartOtpResult,
    │   │   │   │   │                     StartOtpUseCase (interface), StartOtpService
    │   │   │   │   ├── verifyotp/        VerifyOtpCommand, VerifyOtpResult,
    │   │   │   │   │                     VerifyOtpUseCase, VerifyOtpService
    │   │   │   │   ├── seedsuperadmin/   SeedSuperAdminCommand, SeedSuperAdminUseCase,
    │   │   │   │   │                     SeedSuperAdminService
    │   │   │   │   ├── getpublickeys/    GetPublicSigningKeysQuery, GetPublicSigningKeysUseCase,
    │   │   │   │   │                     GetPublicSigningKeysService
    │   │   │   │   └── port/out/         OtpCodeVerifier, TokenIssuer, SigningKeyProvider
    │   │   │   ├── infrastructure/
    │   │   │   │   ├── persistence/
    │   │   │   │   │   ├── repository/   JdbcUserRepositoryAdapter, JdbcOtpChallengeRepositoryAdapter
    │   │   │   │   │   └── mapper/       UserRowMapper, OtpChallengeRowMapper
    │   │   │   │   ├── security/         DevelopmentOtpCodeVerifier, LocalRsaJwtIssuer
    │   │   │   │   ├── seed/             SuperAdminSeeder (runner → SeedSuperAdminUseCase)
    │   │   │   │   └── config/           AuthConfiguration (bean wiring only)
    │   │   │   └── presentation/
    │   │   │       └── rest/
    │   │   │           ├── AuthController, JwkSetController
    │   │   │           ├── request/      StartOtpRequest, VerifyOtpRequest
    │   │   │           ├── response/     StartOtpResponse, VerifyOtpResponse, VerifiedUserResponse
    │   │   │           └── mapper/       AuthApiMapper
    │   │   └── servicemeta/                           ← feature (same four layers)
    │   └── resources/
    │       ├── application.yaml
    │       └── db/migration/             V<n>__<name>.sql (Flyway, forward-only)
    └── test/java/com/buildingos/account/
        ├── architecture/                 ArchitectureTest (ArchUnit)
        ├── auth/
        ├── servicemeta/
        └── support/
```

`building-service` follows the same shape (`building/` feature added with its first code,
plus `servicemeta/`).

## api-gateway

```text
api-gateway/src/main/java/com/buildingos/gateway/
├── GatewayApplication.java
├── routing/
│   ├── config/     GatewayRouteConfig
│   └── filter/     (when needed)
├── security/
│   ├── config/     (when needed)
│   └── filter/     (when needed)
└── common/config/  (when needed)
```

## platform-web (shared kernel)

```text
platform-web/src/main/java/com/buildingos/platform/web/
├── response/     ApiEnvelope, ApiError
├── exception/    PlatformErrorHandler
├── correlation/  CorrelationFilter
├── security/     SecuritySettings
└── config/       PlatformWebConfiguration
```

## Flutter app (`user_app`, BRD §36)

```text
lib/
├── app/          app.dart, config/, routes/, theme/, shell/
├── core/
│   ├── network/      HTTP client, URLs
│   ├── database/     cache client, secure/plain preferences
│   ├── auth/         JWT, session-expiry notifier
│   ├── errors/       failures, exceptions
│   ├── usecases/     base UseCase
│   ├── entities/     shared value objects (e.g. PhoneNumber)
│   ├── controllers/  base controllers
│   ├── services/     navigation, push notifications, utilities
│   ├── utils/        extensions, hooks, helpers
│   └── widgets/      shared widgets
├── features/<feature>/
│   ├── presentation/  pages/, widgets/, controllers/, bindings/, <feature>_pages.dart
│   ├── domain/        entities/ (pure, no JSON), repositories/ (interfaces), usecases/
│   └── data/          models/ (JSON), datasources/ (remote, local), repositories/ (impl), mappers/
└── res/          drawable/, strings/ (resources)
```

Flutter rules: `domain` imports no `flutter`, `get`, `dio`, `data/` or `presentation/`;
`data` never imports `presentation/`; features never import another feature's
internals; `core/network` and `core/database` never import `app/` or `features/`.
Secrets (tokens, user info) are stored with `secure: true`.
New features are scaffolded with `tool/feature_generator` (produces this layout).
