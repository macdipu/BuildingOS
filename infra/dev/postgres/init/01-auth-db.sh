#!/bin/bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    \getenv app_password AUTH_DB_PASSWORD
    CREATE USER auth_app WITH PASSWORD :'app_password';
    CREATE DATABASE auth_db OWNER auth_app;
    REVOKE ALL ON DATABASE auth_db FROM PUBLIC;
    GRANT CONNECT ON DATABASE auth_db TO auth_app;
EOSQL
