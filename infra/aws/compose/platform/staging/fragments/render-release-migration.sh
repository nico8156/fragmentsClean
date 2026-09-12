#!/usr/bin/env bash
# Render a self-contained psql stream; never connect to a database here.
set -Eeuo pipefail
release_dir=${1:?'Usage: render-release-migration.sh <release-directory> <git-revision>'}
source_revision=${2:?'An immutable Git revision is required'}
[[ "$source_revision" =~ ^[0-9a-f]{40}$ ]] || { echo 'Invalid Git revision.' >&2; exit 2; }
driver=app-store-2026-09.psql
[[ -f "$release_dir/$driver" && ! -L "$release_dir/$driver" ]]
files=("$driver")
while IFS= read -r line; do
  if [[ "$line" == '\ir '* ]]; then
    file=${line#'\ir '}
    [[ "$file" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}-[a-z0-9-]+\.sql$ ]] || { echo 'Invalid migration include.' >&2; exit 2; }
    [[ -f "$release_dir/$file" && ! -L "$release_dir/$file" ]]
    # A fragment may not escape the driver's atomic transaction or execute psql commands.
    if grep -Eiq '^[[:space:]]*(BEGIN|START[[:space:]]+TRANSACTION|COMMIT|END|ROLLBACK)[[:space:];]|^[[:space:]]*\\' "$release_dir/$file"; then
      echo 'Migration fragment contains transaction control or psql commands.' >&2
      exit 2
    fi
    files+=("$file")
  fi
done < "$release_dir/$driver"
[[ ${#files[@]} -eq 9 ]] || { echo 'Unexpected release manifest size.' >&2; exit 2; }
checksum=$(
  cd "$release_dir"
  sha256sum "${files[@]}" | sha256sum | awk '{print $1}'
)
printf "\\set release_checksum '%s'\n\\set source_revision '%s'\n" "$checksum" "$source_revision"
while IFS= read -r line || [[ -n "$line" ]]; do
  if [[ "$line" == '\ir '* ]]; then
    file=${line#'\ir '}
    cat "$release_dir/$file"
    printf '\n'
  else
    printf '%s\n' "$line"
  fi
done < "$release_dir/$driver"
