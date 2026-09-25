#!/bin/bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'EOSQL'
    \getenv app_password BACK_OFFICE_DB_PASSWORD
    CREATE USER back_office_app WITH PASSWORD :'app_password';
    CREATE DATABASE back_office_db OWNER back_office_app;
    REVOKE ALL ON DATABASE back_office_db FROM PUBLIC;
    GRANT CONNECT ON DATABASE back_office_db TO back_office_app;
EOSQL
