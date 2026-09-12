#!/usr/bin/env bash
set -euo pipefail

RELEASE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${RELEASE_ROOT}"
command -v rg >/dev/null
./scripts/testcontainers-check.sh
mkdir -p target
RELEASE_REPORTS="$(mktemp -d "${RELEASE_ROOT}/target/release-verification.XXXXXX")"
echo "Fresh release reports: ${RELEASE_REPORTS}"

./scripts/backend-testcontainers \
  -Prelease-verification \
  "-Drelease.reportsDirectory=${RELEASE_REPORTS}" \
  -Dapp.outbox.dispatcher.scheduling-enabled=false \
  -Dspring.datasource.hikari.maximum-pool-size=2 \
  -Dspring.datasource.hikari.minimum-idle=0 \
  test

# A green Maven exit with Docker-dependent tests skipped is not release proof.
if rg -q 'skipped="[1-9][0-9]*"' "${RELEASE_REPORTS}"/TEST-*.xml; then
  echo "Release verification failed: some tests were skipped. Inspect ${RELEASE_REPORTS}." >&2
  exit 1
fi
echo "Release backend verification passed, including infra and vertical tests; no skipped tests."
