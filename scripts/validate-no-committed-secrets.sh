#!/usr/bin/env bash
set -euo pipefail

# Conservative repository guard. Ignored local .env files are excluded and
# matching contents are never printed.

scan_root="${1:-.}"

matches="$(rg -l -U --hidden \
  -g '!.git/**' \
  -g '!**/.env' \
  -g '!**/.env.*' \
  -g '!**/node_modules/**' \
  -g '!**/target/**' \
  -e '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----\r?\n[A-Za-z0-9+/]{32,}={0,2}' \
  -e '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----\\n[A-Za-z0-9+/]{32,}={0,2}' \
  -e 'AKIA[0-9A-Z]{16}' \
  -e 'ASIA[0-9A-Z]{16}' \
  -e 'AIza[0-9A-Za-z_-]{20,}' \
  -e '(^|[=:])[[:space:]]*(sk|rk)-[A-Za-z0-9_-]{20,}' \
  "$scan_root" || true)"

if [[ -n "$matches" ]]; then
  echo "Potential committed secret material found in files:" >&2
  printf '%s\n' "$matches" >&2
  exit 1
fi

echo "No known committed-secret pattern detected."
