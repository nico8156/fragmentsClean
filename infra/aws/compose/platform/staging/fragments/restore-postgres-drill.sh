#!/usr/bin/env bash
set -Eeuo pipefail

backup_uri=${1:?'Usage: restore-postgres-drill.sh s3://bucket/fragments/staging/backups/postgres/file.dump'}
runtime_root=${FRAGMENTS_RUNTIME_ROOT:-/srv/fragments/staging}
environment_file="$runtime_root/.env"

read_environment_value() {
  local key=$1 value
  value=$(awk -v key="$key" '
    index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      if (value ~ /^\047.*\047$/) value = substr(value, 2, length(value) - 2)
      print value; found = 1; exit
    }
    END { if (!found) exit 1 }
  ' "$environment_file") || {
    echo "$key is missing from the Fragments runtime environment." >&2
    return 1
  }
  printf '%s' "$value"
}

POSTGRES_USER=$(read_environment_value POSTGRES_USER)
POSTGRES_BACKUP_S3_BUCKET=$(read_environment_value POSTGRES_BACKUP_S3_BUCKET)
POSTGRES_BACKUP_S3_PREFIX=$(read_environment_value POSTGRES_BACKUP_S3_PREFIX)
AWS_REGION=$(read_environment_value AWS_REGION)

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_BACKUP_S3_BUCKET:?POSTGRES_BACKUP_S3_BUCKET is required}"
: "${POSTGRES_BACKUP_S3_PREFIX:?POSTGRES_BACKUP_S3_PREFIX is required}"
: "${AWS_REGION:?AWS_REGION is required}"

expected_prefix="s3://${POSTGRES_BACKUP_S3_BUCKET}/${POSTGRES_BACKUP_S3_PREFIX%/}/"
if [[ "$backup_uri" != "$expected_prefix"*.dump ]]; then
  echo "Backup URI is outside the configured Fragments backup prefix." >&2
  exit 2
fi

restore_tmp=$(mktemp -d /tmp/fragments-postgres-restore-XXXXXX)
artifact_path="$restore_tmp/backup.dump"
checksum_path="$artifact_path.sha256"
drill_database="fragments_restore_drill_$(date -u +%Y%m%d%H%M%S)_$$"

drop_drill_database() {
  cd "$runtime_root"
  docker compose exec -T fragments-postgres psql \
    --username "$POSTGRES_USER" --dbname postgres --set ON_ERROR_STOP=1 \
    --command "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$drill_database';" >/dev/null 2>&1 || true
  docker compose exec -T fragments-postgres dropdb \
    --username "$POSTGRES_USER" --if-exists "$drill_database" >/dev/null 2>&1 || true
  rm -rf "$restore_tmp"
}
trap drop_drill_database EXIT
umask 077

aws s3 cp "$backup_uri" "$artifact_path" --region "$AWS_REGION" --only-show-errors
aws s3 cp "$backup_uri.sha256" "$checksum_path" --region "$AWS_REGION" --only-show-errors

(
  cd "$restore_tmp"
  expected_checksum=$(awk '{print $1}' "$checksum_path")
  printf '%s  backup.dump\n' "$expected_checksum" > expected.sha256
  sha256sum --check expected.sha256
)

cd "$runtime_root"
docker compose exec -T fragments-postgres createdb \
  --username "$POSTGRES_USER" "$drill_database"
docker compose exec -T fragments-postgres pg_restore \
  --username "$POSTGRES_USER" --dbname "$drill_database" \
  --no-owner --no-privileges --exit-on-error < "$artifact_path"

table_count=$(docker compose exec -T fragments-postgres psql \
  --username "$POSTGRES_USER" --dbname "$drill_database" --tuples-only --no-align \
  --command "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public';")

if [[ ! "$table_count" =~ ^[1-9][0-9]*$ ]]; then
  echo "Restore drill produced no public tables." >&2
  exit 1
fi

FRAGMENTS_RUNTIME_ROOT="$runtime_root" \
  "$runtime_root/replay-account-erasures.sh" "$drill_database"

echo "Restore drill and mandatory account-erasure replay succeeded with $table_count public tables; the temporary database will now be removed."
