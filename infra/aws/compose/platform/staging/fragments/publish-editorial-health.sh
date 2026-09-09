#!/usr/bin/env bash
set -Eeuo pipefail

runtime_root=/srv/fragments/staging
aws_region=eu-west-3
namespace=Fragments/Staging

cd "$runtime_root"
backend_container=$(docker compose ps -q fragments-backend)
status=DEGRADED
if [[ -n "$backend_container" ]]; then
  backend_ip=$(docker inspect "$backend_container" --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
  if [[ -n "$backend_ip" ]]; then
    health=$(curl --fail --silent --show-error "http://$backend_ip:8080/actuator/health" || true)
    component_status=$(jq -r '.components.editorialOperationsHealth.status // empty' <<<"$health")
    if [[ "$component_status" == "UP" ]]; then
      status=UP
    fi
  fi
fi

value=1
[[ "$status" == "UP" ]] && value=0
aws cloudwatch put-metric-data \
  --region "$aws_region" \
  --namespace "$namespace" \
  --metric-name EditorialOperationsDegraded \
  --value "$value" \
  --unit Count

echo "editorial_health_published status=$status value=$value"
