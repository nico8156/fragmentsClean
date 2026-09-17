#!/usr/bin/env bash
set -Eeuo pipefail

drill_database=${1:?'Usage: replay-account-erasures.sh <restored-database>'}
runtime_root=${FRAGMENTS_RUNTIME_ROOT:-/srv/fragments/staging}
environment_file="$runtime_root/.env"
script_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

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
AWS_REGION=$(read_environment_value AWS_REGION)
ACCOUNT_ERASURE_JOURNAL_S3_BUCKET=$(read_environment_value ACCOUNT_ERASURE_JOURNAL_S3_BUCKET)
ACCOUNT_ERASURE_JOURNAL_S3_PREFIX=$(read_environment_value ACCOUNT_ERASURE_JOURNAL_S3_PREFIX)
PRIVATE_MEDIA_S3_BUCKET=$(read_environment_value PRIVATE_MEDIA_S3_BUCKET)

case "$drill_database" in
  fragments_restore_drill_[0-9]*) ;;
  *) echo "Refusing to replay erasures outside an isolated restore database." >&2; exit 2 ;;
esac

replay_tmp=$(mktemp -d /tmp/fragments-erasure-replay-XXXXXX)
trap 'rm -rf "$replay_tmp"' EXIT
umask 077

aws s3 sync \
  "s3://${ACCOUNT_ERASURE_JOURNAL_S3_BUCKET}/${ACCOUNT_ERASURE_JOURNAL_S3_PREFIX%/}/" \
  "$replay_tmp/journal" --region "$AWS_REGION" --only-show-errors

marker_count=0
purge_private_object() {
  local object_key=$1 version_id
  aws s3api delete-object --bucket "$PRIVATE_MEDIA_S3_BUCKET" --key "$object_key" \
    --region "$AWS_REGION" >/dev/null
  while IFS=$'\t' read -r version_id; do
    [[ -n "$version_id" && "$version_id" != "None" ]] || continue
    aws s3api delete-object --bucket "$PRIVATE_MEDIA_S3_BUCKET" --key "$object_key" \
      --version-id "$version_id" --region "$AWS_REGION" >/dev/null
  done < <(aws s3api list-object-versions --bucket "$PRIVATE_MEDIA_S3_BUCKET" \
    --prefix "$object_key" --region "$AWS_REGION" \
    --query "Versions[?Key=='${object_key}'].VersionId | []" --output json | jq -r '.[]')
  while IFS=$'\t' read -r version_id; do
    [[ -n "$version_id" && "$version_id" != "None" ]] || continue
    aws s3api delete-object --bucket "$PRIVATE_MEDIA_S3_BUCKET" --key "$object_key" \
      --version-id "$version_id" --region "$AWS_REGION" >/dev/null
  done < <(aws s3api list-object-versions --bucket "$PRIVATE_MEDIA_S3_BUCKET" \
    --prefix "$object_key" --region "$AWS_REGION" \
    --query "DeleteMarkers[?Key=='${object_key}'].VersionId | []" --output json | jq -r '.[]')
}

while IFS= read -r -d '' marker; do
  IFS=$'\t' read -r request_id user_id auth_user_id requested_at < <(
    jq -er '[.requestId,.userId,.authUserId,.requestedAt] | @tsv' "$marker"
  )

  mapfile -t object_keys < <(
    cd "$runtime_root"
    docker compose exec -T fragments-postgres psql \
      --username "$POSTGRES_USER" --dbname "$drill_database" --tuples-only --no-align \
      --set ON_ERROR_STOP=1 --set "user_id=$user_id" \
      --command "SELECT key FROM (SELECT pending_object_key AS key FROM user_avatar_media WHERE user_id = :'user_id'::uuid UNION SELECT object_key FROM user_avatar_media WHERE user_id = :'user_id'::uuid UNION SELECT pending_object_key FROM experience_media WHERE user_id = :'user_id'::uuid UNION SELECT object_key FROM experience_media WHERE user_id = :'user_id'::uuid) objects WHERE key IS NOT NULL AND key <> '';"
  )
  for object_key in "${object_keys[@]}"; do
    purge_private_object "$object_key"
  done

  cd "$runtime_root"
  docker compose exec -T fragments-postgres psql \
    --username "$POSTGRES_USER" --dbname "$drill_database" \
    --set ON_ERROR_STOP=1 \
    --set "request_id=$request_id" --set "user_id=$user_id" \
    --set "auth_user_id=$auth_user_id" --set "requested_at=$requested_at" \
    < "$script_dir/replay-account-erasures.sql"

  residual=$(docker compose exec -T fragments-postgres psql \
    --username "$POSTGRES_USER" --dbname "$drill_database" --tuples-only --no-align \
    --set ON_ERROR_STOP=1 --set "user_id=$user_id" --set "auth_user_id=$auth_user_id" \
    --command "SELECT CASE WHEN (SELECT count(*) FROM tickets WHERE user_id=:'user_id'::uuid) = 0 AND (SELECT count(*) FROM comments WHERE author_id=:'user_id'::uuid) = 0 AND (SELECT count(*) FROM likes WHERE user_id=:'user_id'::uuid) = 0 AND (SELECT count(*) FROM experiences WHERE user_id=:'user_id'::uuid) = 0 AND (SELECT count(*) FROM saved_coffees WHERE user_id=:'user_id'::uuid) = 0 AND (SELECT count(*) FROM admin_user_access WHERE user_id=:'auth_user_id'::uuid) = 0 AND (SELECT count(*) FROM auth_provider_credentials WHERE user_id=:'auth_user_id'::uuid) = 0 AND (SELECT count(*) FROM refresh_tokens WHERE user_id=:'auth_user_id'::uuid AND revoked=false) = 0 AND (SELECT count(*) FROM auth_users WHERE id=:'auth_user_id'::uuid AND lifecycle_status<>'DELETED') = 0 AND (SELECT count(*) FROM app_users WHERE id=:'user_id'::uuid AND lifecycle_status<>'DELETED') = 0 AND (SELECT count(*) FROM account_erasure_barriers WHERE user_id=:'user_id'::uuid AND status='ERASED') = 5 THEN 0 ELSE 1 END;")
  if [[ "$residual" != "0" ]]; then
    echo "Erasure replay validation failed for request $request_id (residual=$residual)." >&2
    exit 1
  fi
  marker_count=$((marker_count + 1))
done < <(find "$replay_tmp/journal" -type f -name '*.json' -print0 | sort -z)

echo "Account erasure replay succeeded for $marker_count independent journal entries."
