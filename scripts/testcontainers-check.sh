#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required for Testcontainers integration tests." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "docker is installed but not reachable. Start Docker Desktop, Colima, or the configured Docker daemon." >&2
  exit 1
fi

DOCKER_HOST_VALUE="${DOCKER_HOST:-}"
if [[ -z "${DOCKER_HOST_VALUE}" ]]; then
  DOCKER_HOST_VALUE="$(docker context inspect --format '{{.Endpoints.docker.Host}}' 2>/dev/null || true)"
fi
if [[ -z "${DOCKER_HOST_VALUE}" ]]; then
  DOCKER_HOST_VALUE="unix:///var/run/docker.sock"
fi

echo "Docker is reachable."
echo "DOCKER_HOST=${DOCKER_HOST_VALUE}"
if [[ "${DOCKER_HOST_VALUE}" == unix://* ]]; then
  echo "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-/var/run/docker.sock}"
else
  echo "TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-<unset>}"
fi
