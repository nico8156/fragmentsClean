#!/usr/bin/env bash
set -euo pipefail
umask 077

# Render a runtime environment file from one SSM namespace.
# Parameter values are never printed. Run only after the SSM/KMS policy exists.

application="${1:-}"
environment="${2:-}"
output_file="${3:-}"

case "$application" in
  anchor|fragments) ;;
  *) echo "usage: $0 <anchor|fragments> <staging|production> <output-file>" >&2; exit 2 ;;
esac
case "$environment" in
  staging|production) ;;
  *) echo "environment must be staging or production" >&2; exit 2 ;;
esac
case "$output_file" in
  /*) ;;
  *) echo "output-file must be an absolute path" >&2; exit 2 ;;
esac

if [[ -e "$output_file" ]]; then
  echo "refusing to overwrite existing output file: $output_file" >&2
  exit 1
fi

command -v aws >/dev/null || { echo "aws CLI is required" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 1; }

parameter_path="/${application}/${environment}/"
payload_file="$(mktemp)"
output_tmp="${output_file}.tmp.$$"
cleanup() {
  rm -f "$payload_file" "$output_tmp"
}
trap cleanup EXIT

aws ssm get-parameters-by-path \
  --path "$parameter_path" \
  --recursive \
  --with-decryption \
  --output json > "$payload_file"

python3 - "$payload_file" "$output_tmp" "$parameter_path" <<'PY'
import json
import os
import pathlib
import sys

payload_path, output_path, prefix = sys.argv[1:]
with open(payload_path, encoding="utf-8") as source:
    parameters = json.load(source).get("Parameters", [])

if not parameters:
    raise SystemExit(f"no SSM parameters found below {prefix}")

lines = []
for parameter in sorted(parameters, key=lambda item: item["Name"]):
    name = parameter["Name"]
    key = name.removeprefix(prefix)
    if not key or "/" in key or not key.replace("_", "").isalnum():
        raise SystemExit(f"unsupported parameter leaf name: {name}")
    value = parameter["Value"]
    if "\n" in value or "\r" in value:
        raise SystemExit(f"multiline values are not supported in env output: {name}")
    escaped = value.replace("'", "'\\''")
    lines.append(f"{key}='{escaped}'")

pathlib.Path(output_path).write_text("\n".join(lines) + "\n", encoding="utf-8")
os.chmod(output_path, 0o600)
PY

mv "$output_tmp" "$output_file"
trap - EXIT
rm -f "$payload_file"
echo "Rendered runtime environment for $application/$environment to $output_file"
