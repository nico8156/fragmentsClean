#!/usr/bin/env bash
set -Eeuo pipefail

runtime_root=${FRAGMENTS_RUNTIME_ROOT:-/srv/fragments/staging}
environment_file="$runtime_root/.env"

if [[ ! -r "$environment_file" ]]; then
  echo "Fragments runtime environment is unavailable." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$environment_file"
set +a

: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_DB:?POSTGRES_DB is required}"
: "${POSTGRES_BACKUP_S3_BUCKET:?POSTGRES_BACKUP_S3_BUCKET is required}"
: "${POSTGRES_BACKUP_S3_PREFIX:?POSTGRES_BACKUP_S3_PREFIX is required}"
: "${AWS_REGION:?AWS_REGION is required}"

backup_tmp=$(mktemp -d /tmp/fragments-postgres-backup-XXXXXX)
cleanup() { rm -rf "$backup_tmp"; }
trap cleanup EXIT
umask 077

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
artifact="fragments-${timestamp}.dump"
artifact_path="$backup_tmp/$artifact"
checksum_path="$artifact_path.sha256"
destination="s3://${POSTGRES_BACKUP_S3_BUCKET}/${POSTGRES_BACKUP_S3_PREFIX%/}/$artifact"

cd "$runtime_root"
docker compose exec -T fragments-postgres \
  pg_dump --format=custom --no-owner --no-privileges \
  --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" > "$artifact_path"

test -s "$artifact_path"
sha256sum "$artifact_path" > "$checksum_path"

aws s3 cp "$artifact_path" "$destination" \
  --region "$AWS_REGION" --sse AES256 --only-show-errors
aws s3 cp "$checksum_path" "$destination.sha256" \
  --region "$AWS_REGION" --sse AES256 --only-show-errors

echo "PostgreSQL backup uploaded: $destination"
