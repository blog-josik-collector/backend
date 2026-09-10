#!/usr/bin/env bash
# 상용 VM에서 docker-compose.prod.yml 로 서비스 rebuild/기동.
#
# 사용 (저장소 루트):
#   ./scripts/deploy-prod.sh              # 앱 스택 전체
#   ./scripts/deploy-prod.sh all
#   ./scripts/deploy-prod.sh user-service api-gateway
#   ./scripts/deploy-prod.sh all elasticsearch
#
# CI(SSH)에서는 git pull 이후 이 스크립트만 호출하면 된다.
# Bash 3.2+ 호환 (macOS / Linux).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
ALL_APP_SERVICES="user-service interaction-service integrated-api integrated-worker api-gateway"

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "ERROR: ${COMPOSE_FILE} not found in ${ROOT_DIR}" >&2
  exit 1
fi

if [[ ! -f .env ]]; then
  echo "ERROR: .env missing (copy from .env.example and fill secrets)" >&2
  exit 1
fi

RAW=""
if [[ $# -eq 0 ]]; then
  RAW="${ALL_APP_SERVICES}"
else
  for arg in "$@"; do
    case "$arg" in
      "")
        ;;
      all)
        RAW="${RAW} ${ALL_APP_SERVICES}"
        ;;
      *)
        RAW="${RAW} ${arg}"
        ;;
    esac
  done
fi

UNIQUE=""
for s in ${RAW}; do
  case " ${UNIQUE} " in
    *" ${s} "*) ;;
    *) UNIQUE="${UNIQUE} ${s}" ;;
  esac
done
UNIQUE="$(trim "${UNIQUE}")"

if [[ -z "${UNIQUE}" ]]; then
  echo "[deploy-prod] nothing to rebuild — skip"
  exit 0
fi

echo "[deploy-prod] compose=${COMPOSE_FILE} services=${UNIQUE}"
# shellcheck disable=SC2086
docker compose -f "${COMPOSE_FILE}" up -d --build --remove-orphans ${UNIQUE}
echo "[deploy-prod] done"
