#!/bin/bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    \getenv app_password SUBSCRIPTION_DB_PASSWORD
    CREATE USER subscription_app WITH PASSWORD :'app_password';
    CREATE DATABASE subscription_db OWNER subscription_app;
    REVOKE ALL ON DATABASE subscription_db FROM PUBLIC;
    GRANT CONNECT ON DATABASE subscription_db TO subscription_app;
EOSQL
