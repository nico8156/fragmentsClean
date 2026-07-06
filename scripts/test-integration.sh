#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

./scripts/testcontainers-check.sh

exec ./scripts/backend-testcontainers \
  -q \
  -Dapp.outbox.dispatcher.scheduling-enabled=false \
  -Dspring.datasource.hikari.maximum-pool-size=2 \
  -Dspring.datasource.hikari.minimum-idle=0 \
  -Dtest='*IT,*IntegrationTest,*LocalStackTest' \
  test
