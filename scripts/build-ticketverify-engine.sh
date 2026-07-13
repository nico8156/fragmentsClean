#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
ENGINE_DIR="${TICKETVERIFY_ENGINE_DIR:-/Users/nicolasmaldiney/ticketverify-engine}"
BUILD_DIR="${TICKETVERIFY_ENGINE_BUILD_DIR:-$ENGINE_DIR/build}"
TARGET_BIN="$ROOT_DIR/bin/ticketverify"

if [[ ! -f "$ENGINE_DIR/CMakeLists.txt" ]]; then
  echo "ticketverify-engine not found at: $ENGINE_DIR" >&2
  echo "Set TICKETVERIFY_ENGINE_DIR=/path/to/ticketverify-engine" >&2
  exit 1
fi

cmake -S "$ENGINE_DIR" -B "$BUILD_DIR" -DCMAKE_BUILD_TYPE=Release
cmake --build "$BUILD_DIR" --target ticketverify --parallel

mkdir -p "$(dirname "$TARGET_BIN")"
cp "$BUILD_DIR/ticketverify" "$TARGET_BIN"
chmod +x "$TARGET_BIN"

echo "ticketverify synced to $TARGET_BIN"
"$TARGET_BIN" --version >/dev/null
