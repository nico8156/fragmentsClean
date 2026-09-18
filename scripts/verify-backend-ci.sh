#!/usr/bin/env bash
set -euo pipefail

REPOSITORY_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPOSITORY_ROOT"

bash scripts/validate-no-committed-secrets.sh
bash -n infra/aws/compose/platform/staging/fragments/bootstrap-runtime.sh
bash -n infra/aws/compose/platform/staging/fragments/deploy-via-ssm.sh
bash -n infra/aws/compose/platform/staging/fragments/render-release-migration.sh
bash -n scripts/verify-release-health.sh
bash scripts/test-release.sh
./mvnw -q -DskipTests package

echo "Backend CI verification passed."
