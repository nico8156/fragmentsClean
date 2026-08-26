#!/usr/bin/env bash
set -Eeuo pipefail

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_CONTAINER:=fragments-postgres-staging}"

schema_file="${1:?usage: $0 /path/to/schema.sql [/path/to/data.sql]}"
data_file="${2:-}"

if [[ ! -r "$schema_file" ]]; then
  printf 'Schema file is not readable: %s\n' "$schema_file" >&2
  exit 1
fi

until docker exec "$POSTGRES_CONTAINER" pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; do
  sleep 2
done

docker exec -i "$POSTGRES_CONTAINER" psql \
  -v ON_ERROR_STOP=1 \
  -U "$POSTGRES_USER" \
  -d "$POSTGRES_DB" < "$schema_file"

if [[ -n "$data_file" ]]; then
  if [[ ! -r "$data_file" ]]; then
    printf 'Data file is not readable: %s\n' "$data_file" >&2
    exit 1
  fi

  docker exec -i "$POSTGRES_CONTAINER" psql \
    -v ON_ERROR_STOP=1 \
    -U "$POSTGRES_USER" \
    -d "$POSTGRES_DB" < "$data_file"
fi

printf 'Fragments database bootstrap completed for %s.\n' "$POSTGRES_DB"
