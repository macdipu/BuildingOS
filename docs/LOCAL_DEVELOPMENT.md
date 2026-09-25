# Local development — BOS-001 platform foundation

Scope: gateway, auth-service, building-service, subscription-service, their local Postgres/Kafka
infrastructure, and the shared platform-web library, including BOS-010 phone OTP authentication
and the revenue foundation ([REVENUE_MODEL.md](REVENUE_MODEL.md)).

## Prerequisites

- Java 17 (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Docker with Compose v2 (`docker compose version`), daemon running
- fvm + Flutter (only needed for `scripts/verify-flutter.sh`, unrelated to the backend)
- Python 3 with `pyyaml` (only needed for `scripts/check-contracts.py`)

## Environment setup (one-time)

```sh
cp infra/local/.env.example infra/local/.env
# edit infra/local/.env and set real local-only passwords — never commit this file
```

`infra/local/.env` is gitignored. Never commit real credentials; the example file holds
placeholders only. (A separate `infra/dev/` exists with the same shape, pinned image
versions instead of `:latest`, for a shared dev environment.)

Existing checkouts: add `SUBSCRIPTION_DB_PASSWORD=...` to your `infra/local/.env` (new in
BOS-010 F5a). Postgres init scripts only run on a fresh volume; `scripts/verify-platform.sh`
creates `subscription_db` in an existing volume if it is missing.

## Build

```sh
mvn -B -f backend/pom.xml verify
```

Compiles, unit-tests and packages all four modules (`platform-web`, `auth-service`,
`building-service`, `api-gateway`). Security/readiness integration tests use an ephemeral
Testcontainers Postgres and a local fixture JWKS server — no external services required.

## Start everything (recommended)

```sh
docker compose -f infra/local/compose.yaml up -d --build
```

Brings up Postgres, Kafka, MinIO, and all four application services (`auth-service`,
`subscription-service`, `building-service`, `api-gateway`), built from source and wired
together on the compose network, in dependency order (each service's own
`/actuator/health/readiness` gates the next). Images are tagged `buildingos/<service>:local`
and containers named `buildingos-local-<service>` (see [container images](#container-images)
for building without compose).

Ports on the host: gateway `8080`, auth-service `8081`, building-service `8082`,
subscription-service `8083`, Postgres `5432`, Kafka `9092`, MinIO API/console `9000`/`9001`.

Rebuild after a code change:

```sh
mvn -B -f backend/pom.xml package -DskipTests
docker compose -f infra/local/compose.yaml up -d --build <service>
```

Stop everything: see [Stop](#stop).

## Start just the infrastructure

Use this when you want to run one or more services yourself (IDE debugger, faster
edit/restart loop) instead of the fully containerized stack above.

```sh
sh scripts/verify-platform.sh
```

Starts Postgres, Kafka and MinIO via `infra/local/compose.yaml`, waits for health, runs
Flyway migrations (fresh + rerun) for auth-service and building-service, checks that each
service's database role cannot connect to the other's database, round-trips a uniquely
named Kafka smoke topic, then restarts both containers and checks database access. It also
recreates the Kafka container and consumes the same record again to verify named-volume
persistence. A disposable PostgreSQL container checks bootstrap with apostrophes/backslashes
in passwords. Nonzero exit on any failure.

To just bring the infrastructure up without the full check pass:

```sh
docker compose -f infra/local/compose.yaml up -d postgres kafka minio
```

## Run an application outside compose

For fast iteration on one service, run its jar directly instead of rebuilding its
container. Each terminal needs these shared variables for the local auth-service issuer:

```sh
set -a
. infra/local/.env
set +a
export SPRING_PROFILES_ACTIVE=local
export JWT_ISSUER='http://localhost:8081'
export JWT_JWK_SET_URI='http://localhost:8081/.well-known/jwks.json'
export JWT_AUDIENCE='buildingos-local'
```

Auth-service generates a temporary signing key in local/test when no key file is
configured, and development OTP accepts `000000`. Tokens from that key become invalid
after restart. HTTP issuer/JWKS URLs are rejected outside local/test. Production
configuration and the deferred SMS adapter are described in [authentication configuration](AUTH_CONFIGURATION.md).

If auth-service is running via compose (`infra/local/compose.yaml`), stop that container
first (`docker compose -f infra/local/compose.yaml stop auth-service`) to free port 8081,
or run the other services against it as-is (its issuer/JWKS is reachable at
`localhost:8081` either way).

Auth terminal:

```sh
DB_URL=jdbc:postgresql://localhost:5432/auth_db \
DB_USERNAME=auth_app DB_PASSWORD="$AUTH_DB_PASSWORD" SERVER_PORT=8081 \
java -jar backend/auth-service/target/auth-service-0.1.0-SNAPSHOT.jar
```

Building terminal:

```sh
DB_URL=jdbc:postgresql://localhost:5432/building_db \
DB_USERNAME=building_app DB_PASSWORD="$BUILDING_DB_PASSWORD" SERVER_PORT=8082 \
AUTH_SERVICE_URL=http://localhost:8081 SUBSCRIPTION_SERVICE_URL=http://localhost:8083 \
DOCUMENTS_S3_ENDPOINT=http://localhost:9000 DOCUMENTS_S3_PATH_STYLE=true \
DOCUMENTS_S3_ACCESS_KEY="$MINIO_ROOT_USER" DOCUMENTS_S3_SECRET_KEY="$MINIO_ROOT_PASSWORD" \
java -jar backend/building-service/target/building-service-0.1.0-SNAPSHOT.jar
```

Verification documents (BOS-010 F2, D-28) are stored in MinIO bucket `building-documents`, created
private automatically by the `minio` container itself (`MINIO_DEFAULT_BUCKETS`). Console:
http://localhost:9001 (root credentials from `infra/local/.env`). In production leave
`DOCUMENTS_S3_ENDPOINT`/keys empty and point
`DOCUMENTS_S3_BUCKET`/`DOCUMENTS_S3_REGION` at S3; the AWS default credential chain (instance/task
role) is used. Limits: `DOCUMENTS_MAX_SIZE_BYTES` (default 10 MB), `DOCUMENTS_MAX_PER_APPLICATION`
(default 10); PDF/JPEG/PNG only, detected from file content. No virus scanning yet — required before
production. Ownership transfer documents (BOS-010 F4) use the same bucket and content rules under
`ownership-transfers/`, capped by `OWNERSHIP_MAX_DOCUMENTS_PER_TRANSFER` (default 10).

Ownership events (BOS-010 F4, UO-10) are written to `building_outbox` in the same transaction as the
change and published to Kafka by an in-service worker: topic `ownership.transferred` (created on
startup, since broker auto-creation is off), key = unit id, value = the
`contracts/kafka/event-envelope.schema.json` envelope with the same `eventId` on every retry.
`KAFKA_BOOTSTRAP_SERVERS` defaults to `localhost:9092`. While Kafka is down, events stay pending and
retry with exponential backoff (`OUTBOX_RETRY_BASE` PT1S up to `OUTBOX_RETRY_MAX` PT5M); tune the
worker with `OUTBOX_POLL_INTERVAL`, `OUTBOX_BATCH_SIZE`, `OUTBOX_SEND_TIMEOUT`,
`OUTBOX_TOPIC_PARTITIONS`/`OUTBOX_TOPIC_REPLICAS`, or stop it with `OUTBOX_PUBLISHER_ENABLED=false`.
Backlog metrics: `buildingos_outbox_pending`, `buildingos_outbox_oldest_age_seconds`,
`buildingos_outbox_published_total`, `buildingos_outbox_publish_failures_total`.

building-service calls auth-service (`POST /internal/users/provision`, initial building admin, D-29) and
subscription-service (creation-fee status, D-26) directly — not through the gateway — relaying the approving
admin's access token. Both URLs are required; the gateway never routes `/internal/**`.

With all services running, `sh scripts/smoke-building-application.sh` exercises the F2 flow end to end through the
gateway (development OTP; creates smoke data). It stops if the `BUILDING_CREATION` fee is not configured; pass
`SMOKE_SET_FEE=1` only on a throwaway local database to set a test value.

Subscription terminal:

```sh
DB_URL=jdbc:postgresql://localhost:5432/subscription_db \
DB_USERNAME=subscription_app DB_PASSWORD="$SUBSCRIPTION_DB_PASSWORD" SERVER_PORT=8083 \
java -jar backend/subscription-service/target/subscription-service-0.1.0-SNAPSHOT.jar
```

Gateway terminal:

```sh
AUTH_SERVICE_URL=http://localhost:8081 BUILDING_SERVICE_URL=http://localhost:8082 \
SUBSCRIPTION_SERVICE_URL=http://localhost:8083 \
SERVER_PORT=8080 java -jar backend/api-gateway/target/api-gateway-0.1.0-SNAPSHOT.jar
```

Check `/actuator/health/readiness` on ports 8080, 8081, 8082, and 8083. Unauthenticated
requests to gateway `/api/v1/platform/auth` and `/api/v1/platform/building`, or
direct service `/internal/platform/info`, must return 401. A bearer token signed by
your configured issuer with the configured audience is required for metadata; metrics
also require `platform.observe` scope. Stop each application with Ctrl-C.

## Check

```sh
mvn -B -f backend/pom.xml verify              # compile + unit + integration tests
sh scripts/verify-platform.sh                 # Postgres/Kafka isolation + smoke + restart
python3 scripts/check-contracts.py            # OpenAPI + Kafka envelope/event schema sanity
docker compose -f infra/local/compose.yaml config --quiet   # Compose file validity
sh scripts/verify-flutter.sh                  # user_app analyze + test (separate app, may
                                               # report pre-existing failures — see
                                               # agentic/data/project-context/features/BOS-001/BASELINE.md)
```

## Stop

```sh
docker compose -f infra/local/compose.yaml down
```

Named volumes (`postgres_data`, `kafka_data`, `minio_data`) are retained on an ordinary
`down`/`stop`/`restart`. Only `docker compose down -v` deletes them.

## Container images

Compose (`infra/local/compose.yaml`, `infra/dev/compose.yaml`) builds and tags these images
itself. To build one by hand instead:

```sh
mvn -B -f backend/pom.xml package -DskipTests
docker build --tag buildingos/auth-service:local backend/auth-service
docker build --tag buildingos/building-service:local backend/building-service
docker build --tag buildingos/subscription-service:local backend/subscription-service
docker build --tag buildingos/api-gateway:local backend/api-gateway
```

Each image runs as a non-root user on a pinned, digest-referenced `eclipse-temurin` JRE base.

## Troubleshooting

- **Compose fails with "set in infra/local/.env"**: you haven't copied `.env.example` to
  `.env`, or a required variable is unset.
- **Flyway migration fails "role does not exist"**: Postgres init scripts
  (`infra/local/postgres/init/`) only run against a fresh volume. Run
  `docker compose -f infra/local/compose.yaml down -v` to reset, then re-run
  `scripts/verify-platform.sh`.
- **Kafka container exits immediately citing "nonroutable meta-address 0.0.0.0"**: this was
  a KRaft listener misconfiguration fixed during BOS-001; if it recurs, check
  `KAFKA_LISTENERS` in `infra/local/compose.yaml` uses bare `:9092`/`:9093`, not
  `0.0.0.0:...`.
- **MinIO image pull fails**: `quay.io/minio/minio` and Docker Hub's `minio/minio` now deny
  anonymous pulls (an upstream MinIO registry policy change). `infra/local/compose.yaml`
  and `infra/dev/compose.yaml` use `bitnamilegacy/minio` instead — a frozen but still
  pullable, drop-in-compatible build.
- **`scripts/verify-flutter.sh` fails**: check
  `agentic/data/project-context/features/BOS-001/BASELINE.md` first — some failures are
  known pre-existing toolchain issues unrelated to backend work, not regressions.
