#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <health-group-url-or-json-file>" >&2
  exit 2
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "Release health verification requires jq." >&2
  exit 2
fi

source_ref="$1"
if [[ "$source_ref" == http://* || "$source_ref" == https://* ]]; then
  health_json="$(curl --fail --silent --show-error --max-time 20 "$source_ref")"
else
  health_json="$(<"$source_ref")"
fi

if ! jq -e 'type == "object" and (.status | type == "string") and (.components | type == "object")' \
  >/dev/null <<<"$health_json"; then
  echo "Release health payload is malformed." >&2
  exit 1
fi

release_status="$(jq -r '.status' <<<"$health_json")"
failed=0
if [[ "$release_status" != "UP" ]]; then
  echo "release=$release_status" >&2
  failed=1
fi

required_components=(
  db
  messagingRuntimeHealth
  articleAuthoringHealth
  ticketVerificationHealth
  editorialOperationsHealth
)
for component in "${required_components[@]}"; do
  status="$(jq -r --arg component "$component" '.components[$component].status // "MISSING"' <<<"$health_json")"
  if [[ "$status" != "UP" ]]; then
    echo "$component=$status" >&2
    failed=1
  fi
done

if [[ "$failed" -ne 0 ]]; then
  echo "Release health gate failed." >&2
  exit 1
fi

echo "Release health gate passed: all required components are UP."
