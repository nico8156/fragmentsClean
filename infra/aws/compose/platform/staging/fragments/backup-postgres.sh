#!/usr/bin/env bash
set -Eeuo pipefail

runtime_root=${FRAGMENTS_RUNTIME_ROOT:-/srv/fragments/staging}
environment_file="$runtime_root/.env"

if [[ ! -r "$environment_file" ]]; then
  echo "Fragments runtime environment is unavailable." >&2
  exit 1
fi

read_environment_value() {
  local key=$1
  local value

  value=$(awk -v key="$key" '
    index($0, key "=") == 1 {
      print substr($0, length(key) + 2)
      found = 1
      exit
    }
    END { if (!found) exit 1 }
  ' "$environment_file") || {
    echo "$key is missing from the Fragments runtime environment." >&2
    return 1
  }
  printf '%s' "$value"
}

# The runtime file follows Docker Compose dotenv syntax, not shell syntax.
# Reading only the required keys prevents spaces or shell metacharacters in an
# unrelated value from being interpreted as commands by this root service.
POSTGRES_USER=$(read_environment_value POSTGRES_USER)
POSTGRES_DB=$(read_environment_value POSTGRES_DB)
POSTGRES_BACKUP_S3_BUCKET=$(read_environment_value POSTGRES_BACKUP_S3_BUCKET)
POSTGRES_BACKUP_S3_PREFIX=$(read_environment_value POSTGRES_BACKUP_S3_PREFIX)
AWS_REGION=$(read_environment_value AWS_REGION)

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

mapfile -t postgres_containers < <(
  docker ps \
    --filter label=com.docker.compose.service=fragments-postgres \
    --format '{{.Names}}'
)
if [[ ${#postgres_containers[@]} -ne 1 ]]; then
  echo "Expected exactly one running Fragments PostgreSQL container; found ${#postgres_containers[@]}." >&2
  printf 'Candidate: %s\n' "${postgres_containers[@]}" >&2
  exit 1
fi
postgres_container=${postgres_containers[0]}

docker exec -i "$postgres_container" \
  pg_dump --format=custom --no-owner --no-privileges \
  --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" > "$artifact_path"

test -s "$artifact_path"
sha256sum "$artifact_path" > "$checksum_path"

aws s3 cp "$artifact_path" "$destination" \
  --region "$AWS_REGION" --sse AES256 --only-show-errors
aws s3 cp "$checksum_path" "$destination.sha256" \
  --region "$AWS_REGION" --sse AES256 --only-show-errors

echo "PostgreSQL backup uploaded: $destination"
