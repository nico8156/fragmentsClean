#!/usr/bin/env bash
set -eu
tool=${0##*/}
printf '%s %s\n' "$tool" "$*" >> /tmp/deploy-trace
case "$tool" in
  aws)
    if [[ "$SCENARIO" == missing_ssm && "$*" == *APPLE_TEAM_ID* ]]; then exit 1; fi
    printf 'synthetic-value\n'
    ;;
  curl)
    if [[ "$*" == *http://172.19.0.2* ]]; then printf 200; exit 0; fi
    destination=''
    url=''
    while [[ $# -gt 0 ]]; do
      case "$1" in
        --output) destination=$2; shift 2 ;;
        https://raw.githubusercontent.com/*) url=$1; shift ;;
        *) shift ;;
      esac
    done
    path=${url#https://raw.githubusercontent.com/nico8156/fragmentsClean/}
    path=${path#*/}
    cp "/fixture/$path" "$destination"
    ;;
  flock) exit 0 ;;
  systemctl)
    if [[ "$*" == 'start fragments-postgres-backup.service' && "$SCENARIO" == backup_failure* ]]; then exit 1; fi
    ;;
  docker)
    case "$1" in
      ps)
        if [[ "$*" == *service=fragments-postgres* ]]; then printf 'fragments-staging-fragments-postgres-1\n';
        else printf 'staging-fragments-backend-1\n'; fi ;;
      login) cat >/dev/null ;;
      exec)
        cat >/tmp/received-migration.psql
        if [[ "$SCENARIO" == migration_failure ]]; then exit 1; fi
        ;;
      inspect)
        if [[ "$*" == *State.Running* ]]; then
          if [[ "$SCENARIO" == backup_failure_stopped ]]; then printf 'false\n'; else printf 'true\n'; fi
        else printf '172.19.0.2\n'; fi ;;
      compose)
        if [[ "$*" == 'compose ps -q fragments-backend' ]]; then printf 'candidate-backend\n'; fi
        ;;
      pull|start|stop) : ;;
      *) echo 'Unexpected fake Docker operation.' >&2; exit 80 ;;
    esac
    ;;
  *) exit 81 ;;
esac
