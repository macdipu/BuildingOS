#!/bin/bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    \getenv app_password ACCOUNT_DB_PASSWORD
    CREATE USER account_app WITH PASSWORD :'app_password';
    CREATE DATABASE account_db OWNER account_app;
    REVOKE ALL ON DATABASE account_db FROM PUBLIC;
    GRANT CONNECT ON DATABASE account_db TO account_app;
EOSQL
