#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE orderflow_inventory;
    CREATE DATABASE orderflow_fulfillment;
    CREATE DATABASE orderflow_incident;
EOSQL
