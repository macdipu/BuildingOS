# Local development — BOS-001 platform foundation

Scope: gateway, identity-service, building-service, their local Postgres/Kafka infrastructure,
and the shared platform-web library. No business features live here yet.

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

Compiles, unit-tests and packages all four modules (`platform-web`, `identity-service`,
`building-service`, `api-gateway`). Security/readiness integration tests use an ephemeral
Testcontainers Postgres and a local fixture JWKS server — no external services required.

## Start local infrastructure

```sh
sh scripts/verify-platform.sh
```

Starts Postgres and Kafka via `infra/docker/compose.yaml`, waits for health, runs Flyway
migrations (fresh + rerun) for identity-service and building-service, checks that each
service's database role cannot connect to the other's database, round-trips a uniquely
named Kafka smoke topic, then restarts both containers and confirms the data/roles
survived. Nonzero exit on any failure.

To just bring the infrastructure up without the full check pass:

```sh
docker compose -f infra/docker/compose.yaml up -d postgres kafka
```

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
docker build --tag buildingos/identity-service:local backend/identity-service
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
