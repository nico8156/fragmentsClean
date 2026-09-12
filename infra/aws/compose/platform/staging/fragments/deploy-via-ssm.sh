#!/usr/bin/env bash
set -Eeuo pipefail

backend_image=${1:?'Usage: deploy-via-ssm.sh <backend-image> <git-revision>'}
git_revision=${2:?'Usage: deploy-via-ssm.sh <backend-image> <git-revision>'}
[[ "${3:-}" == --approved-staging-release ]] || {
  echo 'Explicit staging release approval is required.' >&2
  exit 2
}
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
[[ "${backend_image##*:sha-}" == "$git_revision" ]] || { echo 'Image/revision mismatch.' >&2; exit 2; }

# Serialize this host's deployments. Do not run against another project/root.
[[ -d "$runtime_root" && -r "$runtime_root/.env" ]]
exec 9>"$runtime_root/.deploy.lock"
flock -n 9 || { echo 'Another Fragments deployment is running.' >&2; exit 2; }
umask 077

deployment_tmp=$(mktemp -d /tmp/fragments-deploy-XXXXXX)
previous_backend=''
previous_backend_running=false
backend_stopped=false
migration_attempted=false
cleanup() {
  local status=$?
  trap - EXIT
  if [[ "$status" -ne 0 && "$backend_stopped" == true ]]; then
    if [[ "$migration_attempted" == false && "$previous_backend_running" == true ]]; then
      docker start "$previous_backend" >/dev/null || echo 'Previous backend restart failed; operator action required.' >&2
    elif [[ "$migration_attempted" == false ]]; then
      echo 'Previously stopped backend remains stopped; no automatic restart.' >&2
    else
      echo 'Migration was attempted: inspect release_schema_history before recovery; no automatic image rollback.' >&2
    fi
  fi
  rm -rf "$deployment_tmp"
  exit "$status"
}
trap cleanup EXIT

download() {
  local source_path=$1
  local destination=$2
  curl --fail --silent --show-error --location \
    "$repository_raw_url/$git_revision/$source_path" \
    --output "$destination"
}

mapfile -t postgres_containers < <(docker ps --filter label=com.docker.compose.service=fragments-postgres --format '{{.Names}}')
mapfile -t backend_containers < <(docker ps -a --filter label=com.docker.compose.service=fragments-backend --format '{{.Names}}')
[[ ${#postgres_containers[@]} -eq 1 && ${#backend_containers[@]} -eq 1 ]] || {
  echo 'Expected exactly one Fragments database and backend; resolve duplicates before release.' >&2; exit 2;
}
postgres_container=${postgres_containers[0]}
previous_backend=${backend_containers[0]}
[[ "$postgres_container" == fragments-staging-fragments-postgres-1 && "$previous_backend" == staging-fragments-backend-1 ]] || {
  echo 'Unexpected staging topology; no service changed.' >&2; exit 2;
}
previous_backend_running=$(docker inspect "$previous_backend" --format '{{.State.Running}}')
[[ "$previous_backend_running" == true || "$previous_backend_running" == false ]] || exit 2

mkdir -p "$deployment_tmp/release"
download infra/aws/compose/platform/staging/fragments/docker-compose.yml "$deployment_tmp/docker-compose.yml"
download infra/aws/compose/platform/staging/fragments/bootstrap-runtime.sh "$deployment_tmp/bootstrap-runtime.sh"
download infra/aws/compose/platform/staging/fragments/backup-postgres.sh "$deployment_tmp/backup-postgres.sh"
download infra/aws/compose/platform/staging/fragments/restore-postgres-drill.sh "$deployment_tmp/restore-postgres-drill.sh"
download infra/aws/compose/platform/staging/fragments/fragments-postgres-backup.service "$deployment_tmp/fragments-postgres-backup.service"
download infra/aws/compose/platform/staging/fragments/fragments-postgres-backup.timer "$deployment_tmp/fragments-postgres-backup.timer"
download infra/aws/compose/platform/staging/fragments/publish-editorial-health.sh "$deployment_tmp/publish-editorial-health.sh"
download infra/aws/compose/platform/staging/fragments/fragments-editorial-health.service "$deployment_tmp/fragments-editorial-health.service"
download infra/aws/compose/platform/staging/fragments/fragments-editorial-health.timer "$deployment_tmp/fragments-editorial-health.timer"
download infra/aws/compose/platform/staging/fragments/render-release-migration.sh "$deployment_tmp/render-release-migration.sh"
download src/main/resources/db/release/app-store-2026-09.psql "$deployment_tmp/release/app-store-2026-09.psql"
while IFS= read -r line; do
  if [[ "$line" == '\ir '* ]]; then
    migration_file=${line#'\ir '}
    [[ "$migration_file" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}-[a-z0-9-]+\.sql$ ]] || exit 2
    download "src/main/resources/db/release/$migration_file" "$deployment_tmp/release/$migration_file"
  fi
done < "$deployment_tmp/release/app-store-2026-09.psql"
bash "$deployment_tmp/render-release-migration.sh" "$deployment_tmp/release" "$git_revision" > "$deployment_tmp/migration.psql"

# Resolve every SSM prerequisite and validate Compose before changing the live config or stopping a writer.
bash "$deployment_tmp/bootstrap-runtime.sh" "$backend_image" "$deployment_tmp"
docker compose --env-file "$deployment_tmp/.env" \
  -f "$deployment_tmp/docker-compose.yml" config --quiet > "$deployment_tmp/config.log" 2>&1 || {
    echo 'Candidate Compose configuration is invalid; values withheld.' >&2; exit 1;
  }
registry=${backend_image%%/*}
aws ecr get-login-password --region "$aws_region" | docker login --username AWS --password-stdin "$registry" >/dev/null
docker pull "$backend_image" >/dev/null

install -m 0700 "$deployment_tmp/bootstrap-runtime.sh" "$runtime_root/bootstrap-runtime.sh"
install -m 0700 "$deployment_tmp/backup-postgres.sh" "$runtime_root/backup-postgres.sh"
install -m 0700 "$deployment_tmp/restore-postgres-drill.sh" "$runtime_root/restore-postgres-drill.sh"
install -m 0644 "$deployment_tmp/fragments-postgres-backup.service" /etc/systemd/system/fragments-postgres-backup.service
install -m 0644 "$deployment_tmp/fragments-postgres-backup.timer" /etc/systemd/system/fragments-postgres-backup.timer
install -m 0700 "$deployment_tmp/publish-editorial-health.sh" "$runtime_root/publish-editorial-health.sh"
install -m 0644 "$deployment_tmp/fragments-editorial-health.service" /etc/systemd/system/fragments-editorial-health.service
install -m 0644 "$deployment_tmp/fragments-editorial-health.timer" /etc/systemd/system/fragments-editorial-health.timer

systemctl daemon-reload
systemctl enable --now fragments-postgres-backup.timer

# Stop the one resolved writer/consumer, never PostgreSQL, Anchor or the shared network.
docker stop --time 60 "$previous_backend" >/dev/null
backend_stopped=true

# A deployment is a natural recovery boundary. Refuse to mutate the schema if
# the pre-deployment backup cannot be produced and uploaded.
if ! systemctl start fragments-postgres-backup.service; then
  echo "Pre-deployment PostgreSQL backup failed; schema unchanged, preserving the previous backend running state." >&2
  exit 1
fi

migration_attempted=true
if ! docker exec -i "$postgres_container" sh -c \
  'exec psql -X -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < "$deployment_tmp/migration.psql" > "$deployment_tmp/migration.log" 2>&1; then
  echo 'Migration failed or acknowledgement was lost; backend remains stopped. SQL output withheld.' >&2
  exit 1
fi

install -m 0600 "$deployment_tmp/.env" "$runtime_root/.env"
install -m 0644 "$deployment_tmp/docker-compose.yml" "$runtime_root/docker-compose.yml"

cd "$runtime_root"
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
  docker compose stop fragments-backend
  echo "Fragments backend health check failed with HTTP ${health_status:-none}." >&2
  exit 1
fi

systemctl enable --now fragments-editorial-health.timer

echo "Fragments staging deployed successfully: $backend_image"
