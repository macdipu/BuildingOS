#!/bin/bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    \getenv app_password IDENTITY_DB_PASSWORD
    CREATE USER identity_app WITH PASSWORD :'app_password';
    CREATE DATABASE identity_db OWNER identity_app;
    REVOKE ALL ON DATABASE identity_db FROM PUBLIC;
    GRANT CONNECT ON DATABASE identity_db TO identity_app;
EOSQL
