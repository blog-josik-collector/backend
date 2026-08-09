#!/usr/bin/env bash
# 실행 중인 서비스의 OpenAPI 스펙을 docs/api/*.json 으로 덤프한다.
#
# 사용법:
#   1. 덤프할 서비스를 로컬에서 기동한다.
#   2. 저장소 루트에서 실행한다.
#        ./scripts/dump-openapi.sh              # 세 서비스 전부
#        ./scripts/dump-openapi.sh interaction  # 특정 서비스만
#
# 환경 변수로 호스트/포트를 덮어쓸 수 있다.
#   OPENAPI_HOST=localhost
#   USER_SERVICE_PORT=8080
#   INTEGRATED_API_PORT=8081
#   INTERACTION_SERVICE_PORT=8083

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/docs/api"
HOST="${OPENAPI_HOST:-localhost}"

USER_SERVICE_PORT="${USER_SERVICE_PORT:-8080}"
INTEGRATED_API_PORT="${INTEGRATED_API_PORT:-8081}"
INTERACTION_SERVICE_PORT="${INTERACTION_SERVICE_PORT:-8083}"

mkdir -p "${OUT_DIR}"

dump() {
  local name="$1"
  local url="$2"
  local out="$3"
  local raw

  echo "→ ${name}: ${url}"
  if ! raw="$(curl -sf --max-time 10 "${url}")"; then
    echo "ERROR: failed to fetch OpenAPI from ${url}" >&2
    echo "       Is ${name} running? (check port / springdoc path)" >&2
    return 1
  fi

  if ! printf '%s' "${raw}" | python3 -m json.tool --sort-keys > "${out}.tmp"; then
    rm -f "${out}.tmp"
    echo "ERROR: response from ${url} is not valid JSON" >&2
    return 1
  fi

  mv "${out}.tmp" "${out}"
  echo "  generated: ${out#"${ROOT_DIR}/"}"
}

dump_user() {
  dump "user-service" \
    "http://${HOST}:${USER_SERVICE_PORT}/users/v3/api-docs/user-v1" \
    "${OUT_DIR}/user-service-openapi.json"
}

dump_integrated() {
  dump "integrated-api" \
    "http://${HOST}:${INTEGRATED_API_PORT}/integrated-api/v3/api-docs/integrated-v1" \
    "${OUT_DIR}/integrated-api-openapi.json"
}

dump_interaction() {
  dump "interaction-service" \
    "http://${HOST}:${INTERACTION_SERVICE_PORT}/interaction/v3/api-docs/interaction-v1" \
    "${OUT_DIR}/interaction-service-openapi.json"
}

TARGET="${1:-all}"
case "${TARGET}" in
  all)
    dump_user
    dump_integrated
    dump_interaction
    ;;
  user|user-service)
    dump_user
    ;;
  integrated|integrated-api)
    dump_integrated
    ;;
  interaction|interaction-service)
    dump_interaction
    ;;
  -h|--help|help)
    sed -n '2,16p' "$0"
    exit 0
    ;;
  *)
    echo "Unknown target: ${TARGET}" >&2
    echo "Usage: $0 [all|user|integrated|interaction]" >&2
    exit 1
    ;;
esac

echo "done."
