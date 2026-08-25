#!/usr/bin/env bash
set -euo pipefail

# Creates the shared-host directory layout without touching existing data.
# It intentionally does not format, mount, delete, chown, or start anything.

platform_root="${PLATFORM_ROOT:-/srv/platform}"
anchor_root="${ANCHOR_RUNTIME_ROOT:-/srv/anchor/staging}"
fragments_root="${FRAGMENTS_RUNTIME_ROOT:-/srv/fragments/staging}"
anchor_postgres_root="${ANCHOR_POSTGRES_DATA_ROOT:-/srv/anchor-postgres/postgres-data}"
fragments_postgres_root="${FRAGMENTS_POSTGRES_DATA_ROOT:-/srv/fragments-postgres/postgres-data}"
fragments_photos_root="${FRAGMENTS_COFFEE_PHOTOS_ROOT:-/srv/fragments/staging/coffee-photos}"

for path in "$platform_root" "$anchor_root" "$fragments_root" "$anchor_postgres_root" "$fragments_postgres_root" "$fragments_photos_root"; do
  case "$path" in
    /*) ;;
    *) echo "Path must be absolute: $path" >&2; exit 1 ;;
  esac
done

install -d \
  "$platform_root/caddy" \
  "$platform_root/scripts" \
  "$anchor_root/db" \
  "$fragments_root/db" \
  "$anchor_root/apple-wallet-certs" \
  "$fragments_root/coffee-photos" \
  "$anchor_postgres_root" \
  "$fragments_postgres_root" \
  "$fragments_photos_root"

echo "Shared staging layout is ready. No existing data was changed."
