# Local development — BOS-001 platform foundation

Scope: gateway, account-service, building-service, their local Postgres/Kafka infrastructure,
and the shared platform-web library, including BOS-010 phone OTP authentication.

## Prerequisites

- Java 17 (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Docker with Compose v2 (`docker compose version`), daemon running
- fvm + Flutter (only needed for `scripts/verify-flutter.sh`, unrelated to the backend)
- Python 3 with `pyyaml` (only needed for `scripts/check-contracts.py`)

## Environment setup (one-time)

```sh
cp infra/docker/.env.example infra/docker/.env
# edit infra/docker/.env and set real local-only passwords — never commit this file
```

`infra/docker/.env` is gitignored. Never commit real credentials; the example file holds
placeholders only.

## Build

```sh
mvn -B -f backend/pom.xml verify
```

Compiles, unit-tests and packages all four modules (`platform-web`, `account-service`,
`building-service`, `api-gateway`). Security/readiness integration tests use an ephemeral
Testcontainers Postgres and a local fixture JWKS server — no external services required.

## Start local infrastructure

```sh
sh scripts/verify-platform.sh
```

Starts Postgres and Kafka via `infra/docker/compose.yaml`, waits for health, runs Flyway
migrations (fresh + rerun) for account-service and building-service, checks that each
service's database role cannot connect to the other's database, round-trips a uniquely
named Kafka smoke topic, then restarts both containers and checks database access. It also recreates the Kafka
container and consumes the same record again to verify named-volume persistence. A
disposable PostgreSQL container checks bootstrap with apostrophes/backslashes in passwords. Nonzero exit on any failure.

To just bring the infrastructure up without the full check pass:

```sh
docker compose -f infra/docker/compose.yaml up -d postgres kafka
```

## Start the applications

Compose runs infrastructure only. After the build and infrastructure check, run the
three application JARs in separate terminals from the repository root. Each terminal
needs these shared variables for the local account-service issuer:

```sh
set -a
. infra/docker/.env
set +a
export SPRING_PROFILES_ACTIVE=local
export JWT_ISSUER='http://localhost:8081'
export JWT_JWK_SET_URI='http://localhost:8081/.well-known/jwks.json'
export JWT_AUDIENCE='buildingos-local'
```

Account-service generates a temporary signing key in local/test when no key file is
configured, and development OTP accepts `000000`. Tokens from that key become invalid
after restart. HTTP issuer/JWKS URLs are rejected outside local/test. Production
configuration and the deferred SMS adapter are described in [authentication configuration](AUTH_CONFIGURATION.md).

Account terminal:

```sh
DB_URL=jdbc:postgresql://localhost:5432/account_db \
DB_USERNAME=account_app DB_PASSWORD="$ACCOUNT_DB_PASSWORD" SERVER_PORT=8081 \
java -jar backend/account-service/target/account-service-0.1.0-SNAPSHOT.jar
```

Building terminal:

```sh
DB_URL=jdbc:postgresql://localhost:5432/building_db \
DB_USERNAME=building_app DB_PASSWORD="$BUILDING_DB_PASSWORD" SERVER_PORT=8082 \
java -jar backend/building-service/target/building-service-0.1.0-SNAPSHOT.jar
```

Gateway terminal:

```sh
ACCOUNT_SERVICE_URL=http://localhost:8081 BUILDING_SERVICE_URL=http://localhost:8082 \
SERVER_PORT=8080 java -jar backend/api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar
```

Check `/actuator/health/readiness` on ports 8080, 8081, and 8082. Unauthenticated
requests to gateway `/api/v1/platform/account` and `/api/v1/platform/building`, or
direct service `/internal/platform/info`, must return 401. A bearer token signed by
your configured issuer with the configured audience is required for metadata; metrics
also require `platform.observe` scope. Stop each application with Ctrl-C.

## Check

```sh
mvn -B -f backend/pom.xml verify              # compile + unit + integration tests
sh scripts/verify-platform.sh                 # Postgres/Kafka isolation + smoke + restart
python3 scripts/check-contracts.py            # OpenAPI + Kafka envelope schema sanity
docker compose -f infra/docker/compose.yaml config --quiet   # Compose file validity
sh scripts/verify-flutter.sh                  # user_app analyze + test (separate app, may
                                               # report pre-existing failures — see
                                               # agentic/data/project-context/features/BOS-001/BASELINE.md)
```

## Stop

```sh
docker compose -f infra/docker/compose.yaml down
```

Named volumes (`postgres_data`, `kafka_data`) are retained on an ordinary `down`/`stop`/
`restart`. Only `docker compose down -v` deletes them.

## Container images

```sh
mvn -B -f backend/pom.xml package -DskipTests
docker build --tag buildingos/account-service:local backend/account-service
docker build --tag buildingos/building-service:local backend/building-service
docker build --tag buildingos/api-gateway:local backend/api-gateway
```

Each image runs as a non-root user on a pinned, digest-referenced `eclipse-temurin` JRE base.

## Troubleshooting

- **Compose fails with "set in infra/docker/.env"**: you haven't copied `.env.example` to
  `.env`, or a required variable is unset.
- **Flyway migration fails "role does not exist"**: Postgres init scripts
  (`infra/docker/postgres/init/`) only run against a fresh volume. Run
  `docker compose -f infra/docker/compose.yaml down -v` to reset, then re-run
  `scripts/verify-platform.sh`.
- **Kafka container exits immediately citing "nonroutable meta-address 0.0.0.0"**: this was
  a KRaft listener misconfiguration fixed during BOS-001; if it recurs, check
  `KAFKA_LISTENERS` in `infra/docker/compose.yaml` uses bare `:9092`/`:9093`, not
  `0.0.0.0:...`.
- **`scripts/verify-flutter.sh` fails**: check
  `agentic/data/project-context/features/BOS-001/BASELINE.md` first — some failures are
  known pre-existing toolchain issues unrelated to backend work, not regressions.
