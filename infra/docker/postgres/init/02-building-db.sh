#!/bin/bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER building_app WITH PASSWORD '${BUILDING_DB_PASSWORD}';
    CREATE DATABASE building_db OWNER building_app;
    REVOKE ALL ON DATABASE building_db FROM PUBLIC;
    GRANT CONNECT ON DATABASE building_db TO building_app;
EOSQL
