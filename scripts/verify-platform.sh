#!/bin/sh
# Deterministic local verification for the BOS-001 platform foundation.
# Nonzero exit on any missing prerequisite or failing check. No business data is created.
set -eu

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

COMPOSE="docker compose -f infra/docker/compose.yaml"
ENV_FILE="infra/docker/.env"

fail() { echo "VERIFY FAILED: $1" >&2; exit 1; }

command -v docker >/dev/null 2>&1 || fail "docker is required"
command -v mvn >/dev/null 2>&1 || fail "mvn is required"
[ -f "$ENV_FILE" ] || fail "$ENV_FILE is missing; copy infra/docker/.env.example and set local passwords"

# shellcheck disable=SC1090
. "$ENV_FILE"
export POSTGRES_SUPERUSER_PASSWORD ACCOUNT_DB_PASSWORD BUILDING_DB_PASSWORD

echo "==> Starting Postgres and Kafka"
$COMPOSE up -d postgres kafka

wait_healthy() {
    service="$1"
    tries=30
    while [ "$tries" -gt 0 ]; do
        status=$($COMPOSE ps --format json "$service" 2>/dev/null | python3 -c "import json,sys; lines=[l for l in sys.stdin if l.strip()]; print(json.loads(lines[0])['Health'] if lines else '')" 2>/dev/null || echo "")
        [ "$status" = "healthy" ] && return 0
        tries=$((tries - 1))
        sleep 2
    done
    fail "$service did not become healthy"
}
wait_healthy postgres
wait_healthy kafka
echo "==> Postgres and Kafka are healthy"

# Exercise the actual bootstrap scripts against a disposable cluster with quoted
# passwords. The project databases and volumes are never reset by this check.
BOOTSTRAP_CONTAINER="buildingos-bootstrap-check-$$"
cleanup() {
    docker rm -fv "$BOOTSTRAP_CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' HUP TERM
POSTGRES_IMAGE=$($COMPOSE images -q postgres)
TEST_PASSWORD="bootstrap'quote\\slash"
docker run -d --name "$BOOTSTRAP_CONTAINER" --network none \
    -e POSTGRES_PASSWORD=bootstrap-test-only \
    -e "ACCOUNT_DB_PASSWORD=$TEST_PASSWORD" -e "BUILDING_DB_PASSWORD=$TEST_PASSWORD" \
    -v "$ROOT_DIR/infra/docker/postgres/init:/docker-entrypoint-initdb.d:ro" \
    "$POSTGRES_IMAGE" >/dev/null
tries=30
while ! docker exec "$BOOTSTRAP_CONTAINER" pg_isready -h localhost -U postgres >/dev/null 2>&1; do
    tries=$((tries - 1))
    [ "$tries" -gt 0 ] || fail "quoted-password bootstrap did not become ready"
    sleep 2
done
for domain in account building; do
    docker exec -e "PGPASSWORD=$TEST_PASSWORD" "$BOOTSTRAP_CONTAINER" \
        psql -h localhost -U "${domain}_app" -d "${domain}_db" -v ON_ERROR_STOP=1 \
        -tAc 'select 1' >/dev/null || fail "quoted-password bootstrap failed for $domain"
done
cleanup
trap - EXIT HUP INT TERM
echo "==> Quoted-password bootstrap passed"

echo "==> Running and re-running Flyway migrations (account-service)"
mvn -q -B -f backend/pom.xml -pl account-service flyway:migrate -Dflyway.password="$ACCOUNT_DB_PASSWORD"
mvn -q -B -f backend/pom.xml -pl account-service flyway:migrate -Dflyway.password="$ACCOUNT_DB_PASSWORD"

echo "==> Running and re-running Flyway migrations (building-service)"
mvn -q -B -f backend/pom.xml -pl building-service flyway:migrate -Dflyway.password="$BUILDING_DB_PASSWORD"
mvn -q -B -f backend/pom.xml -pl building-service flyway:migrate -Dflyway.password="$BUILDING_DB_PASSWORD"

psql_as() {
    role="$1"; password="$2"; db="$3"
    PGPASSWORD="$password" $COMPOSE exec -T -e PGPASSWORD postgres \
        psql -h localhost -U "$role" -d "$db" -tAc "select 1" >/tmp/verify-psql.out 2>&1
}

echo "==> Checking database role isolation"
psql_as account_app "$ACCOUNT_DB_PASSWORD" account_db || fail "account_app could not connect to account_db"
psql_as building_app "$BUILDING_DB_PASSWORD" building_db || fail "building_app could not connect to building_db"
if psql_as account_app "$ACCOUNT_DB_PASSWORD" building_db; then
    fail "account_app was able to connect to building_db (isolation broken)"
fi
if psql_as building_app "$BUILDING_DB_PASSWORD" account_db; then
    fail "building_app was able to connect to account_db (isolation broken)"
fi
echo "==> Database role isolation confirmed"

echo "==> Kafka broker smoke round trip"
TOPIC="platform-smoke-$(date +%s)-$$"
$COMPOSE exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --create --topic "$TOPIC" --partitions 1 --replication-factor 1 >/dev/null
SENT="smoke-$(date +%s)-$$"
printf '%s\n' "$SENT" | $COMPOSE exec -T kafka /opt/kafka/bin/kafka-console-producer.sh \
    --bootstrap-server localhost:9092 --topic "$TOPIC" >/dev/null
RECEIVED=$($COMPOSE exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 --topic "$TOPIC" --from-beginning --max-messages 1 --timeout-ms 15000 2>/dev/null || true)
[ "$RECEIVED" = "$SENT" ] || fail "kafka smoke round trip payload mismatch"
echo "==> Kafka broker smoke round trip passed"

echo "==> Restarting Postgres and Kafka to verify state persists"
$COMPOSE restart postgres kafka
wait_healthy postgres
wait_healthy kafka
psql_as account_app "$ACCOUNT_DB_PASSWORD" account_db || fail "account_app lost access to account_db after restart"
psql_as building_app "$BUILDING_DB_PASSWORD" building_db || fail "building_app lost access to building_db after restart"
# A restart preserves the container writable layer too. Recreate the broker to
# prove the named volume actually holds its records, then consume the same data.
$COMPOSE up -d --force-recreate kafka
wait_healthy kafka
RECEIVED=$($COMPOSE exec -T kafka /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server localhost:9092 --topic "$TOPIC" --from-beginning --max-messages 1 --timeout-ms 15000 2>/dev/null || true)
[ "$RECEIVED" = "$SENT" ] || fail "Kafka record did not survive container recreation"
$COMPOSE exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 \
    --delete --topic "$TOPIC" >/dev/null
echo "==> Restart and Kafka volume persistence confirmed"

echo "VERIFY PASSED"
