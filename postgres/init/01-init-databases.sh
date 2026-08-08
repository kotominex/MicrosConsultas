#!/bin/bash
# Se ejecuta automaticamente al primer arranque del contenedor de Postgres
# (docker-entrypoint-initdb.d). Crea una base de datos por microservicio,
# ya que el stack usa una unica instancia de Postgres con bases separadas
# por servicio (ver ARQUITECTURA.md, seccion 2.5).
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE auth_db;
    CREATE DATABASE core_db;
EOSQL
