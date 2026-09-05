#!/usr/bin/env bash
set -Eeuo pipefail

backend_image=${1:?'Usage: deploy-via-ssm.sh <backend-image> <git-revision>'}
git_revision=${2:?'Usage: deploy-via-ssm.sh <backend-image> <git-revision>'}
runtime_root=/srv/fragments/staging
aws_region=eu-west-3
repository_raw_url=https://raw.githubusercontent.com/nico8156/fragmentsClean

if [[ ! "$git_revision" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Invalid immutable Git revision." >&2
  exit 2
fi
if [[ ! "$backend_image" =~ ^851725375299\.dkr\.ecr\.eu-west-3\.amazonaws\.com/fragments/staging/backend:sha-[0-9a-f]{40}$ ]]; then
  echo "Unexpected backend image reference." >&2
  exit 2
fi

deployment_tmp=$(mktemp -d /tmp/fragments-deploy-XXXXXX)
cleanup() { rm -rf "$deployment_tmp"; }
trap cleanup EXIT

download() {
  local source_path=$1
  local destination=$2
  curl --fail --silent --show-error --location \
    "$repository_raw_url/$git_revision/$source_path" \
    --output "$destination"
}

mkdir -p "$runtime_root/db" "$runtime_root/coffee-photos"
download infra/aws/compose/platform/staging/fragments/docker-compose.yml "$deployment_tmp/docker-compose.yml"
download infra/aws/compose/platform/staging/fragments/bootstrap-runtime.sh "$deployment_tmp/bootstrap-runtime.sh"
download infra/aws/compose/platform/staging/fragments/backup-postgres.sh "$deployment_tmp/backup-postgres.sh"
download infra/aws/compose/platform/staging/fragments/restore-postgres-drill.sh "$deployment_tmp/restore-postgres-drill.sh"
download infra/aws/compose/platform/staging/fragments/fragments-postgres-backup.service "$deployment_tmp/fragments-postgres-backup.service"
download infra/aws/compose/platform/staging/fragments/fragments-postgres-backup.timer "$deployment_tmp/fragments-postgres-backup.timer"
download src/main/resources/schema.sql "$deployment_tmp/schema.sql"

install -m 0644 "$deployment_tmp/docker-compose.yml" "$runtime_root/docker-compose.yml"
install -m 0644 "$deployment_tmp/schema.sql" "$runtime_root/db/schema.sql"
install -m 0700 "$deployment_tmp/bootstrap-runtime.sh" "$runtime_root/bootstrap-runtime.sh"
install -m 0700 "$deployment_tmp/backup-postgres.sh" "$runtime_root/backup-postgres.sh"
install -m 0700 "$deployment_tmp/restore-postgres-drill.sh" "$runtime_root/restore-postgres-drill.sh"
install -m 0644 "$deployment_tmp/fragments-postgres-backup.service" /etc/systemd/system/fragments-postgres-backup.service
install -m 0644 "$deployment_tmp/fragments-postgres-backup.timer" /etc/systemd/system/fragments-postgres-backup.timer

"$runtime_root/bootstrap-runtime.sh" "$backend_image"
systemctl daemon-reload
systemctl enable --now fragments-postgres-backup.timer

registry=${backend_image%%/*}
aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$registry" >/dev/null

postgres_container=$(docker ps \
  --filter label=com.docker.compose.service=fragments-postgres \
  --format '{{.Names}}' \
  | head -n 1)
if [[ -z "$postgres_container" ]]; then
  echo "The existing Fragments PostgreSQL container is not running." >&2
  exit 1
fi

# A deployment is a natural recovery boundary. Refuse to mutate the schema if
# the pre-deployment backup cannot be produced and uploaded.
systemctl start fragments-postgres-backup.service

docker exec -i "$postgres_container" sh -lc \
  'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < "$runtime_root/db/schema.sql" >/dev/null

cd "$runtime_root"
docker compose pull fragments-backend
docker compose up -d --no-deps --force-recreate fragments-backend

backend_container=$(docker compose ps -q fragments-backend)
if [[ -z "$backend_container" ]]; then
  echo "The Fragments backend container was not created." >&2
  exit 1
fi

health_status=''
for _ in $(seq 1 30); do
  backend_ip=$(docker inspect "$backend_container" --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
  if [[ -n "$backend_ip" ]]; then
    health_status=$(curl --silent --output /dev/null --write-out '%{http_code}' \
      "http://$backend_ip:8080/actuator/health" || true)
  fi
  if [[ "$health_status" == '200' ]]; then
    break
  fi
  sleep 5
done

if [[ "$health_status" != '200' ]]; then
  docker compose logs --tail=120 fragments-backend
  echo "Fragments backend health check failed with HTTP ${health_status:-none}." >&2
  exit 1
fi

# Remove only the obsolete duplicate backend from the former Compose project.
# PostgreSQL and its persistent volume remain untouched.
if docker inspect fragments-staging-fragments-backend-1 >/dev/null 2>&1; then
  docker rm -f fragments-staging-fragments-backend-1 >/dev/null
fi

echo "Fragments staging deployed successfully: $backend_image"
