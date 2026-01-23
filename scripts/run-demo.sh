#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

docker compose up -d
export SPRING_PROFILES_ACTIVE=demo
mvn spring-boot:run

