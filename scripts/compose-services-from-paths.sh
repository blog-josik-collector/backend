#!/usr/bin/env bash
# 변경된 파일 경로 목록 → docker-compose.prod.yml 에서 rebuild 할 서비스 이름.
#
# 사용:
#   ./scripts/compose-services-from-paths.sh path1 path2 ...
#   git diff --name-only HEAD~1 HEAD | ./scripts/compose-services-from-paths.sh
#
# 출력 (한 줄, 공백 구분):
#   all                         # 루트/도커/공통 빌드 영향 → 앱 스택 전체
#   user-service api-gateway    # 해당 서비스만
#   (빈 줄)                     # 배포 대상 없음 (docs 등)
#
# Bash 3.2+ 호환 (macOS / Linux).

set -euo pipefail

ALL_APP_SERVICES="user-service interaction-service integrated-api integrated-worker api-gateway"

trim() {
  # leading/trailing whitespace
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

WANT_LIST=""

has_want() {
  case " ${WANT_LIST} " in
    *" $1 "*) return 0 ;;
    *) return 1 ;;
  esac
}

add() {
  if ! has_want "$1"; then
    WANT_LIST="${WANT_LIST} $1"
  fi
}

mark_all() {
  local s
  for s in ${ALL_APP_SERVICES}; do
    add "$s"
  done
}

classify() {
  local f="$1"
  f="${f#./}"

  case "$f" in
    docs/*|*.md|.gitignore|.dockerignore|.env.example|.env.dev.example)
      return 0
      ;;
    scripts/backup-postgres.sh|scripts/dump-openapi.sh|scripts/check-coverage.sh)
      return 0
      ;;
    .github/workflows/ci.yml)
      return 0
      ;;

    build.gradle|settings.gradle|gradle.properties|gradle/*|gradlew|gradlew.bat)
      mark_all
      return 0
      ;;
    docker/*|docker-compose.prod.yml)
      mark_all
      return 0
      ;;

    docker-backend/elasticsearch/*)
      add elasticsearch
      return 0
      ;;

    common-data-access/*|common-web/*|common-logging/*)
      add user-service
      add interaction-service
      add integrated-api
      add integrated-worker
      return 0
      ;;
    common-elasticsearch/*)
      add interaction-service
      add integrated-api
      add integrated-worker
      return 0
      ;;

    user-service/*) add user-service ;;
    interaction-service/*) add interaction-service ;;
    integrated-api/*) add integrated-api ;;
    integrated-worker/*) add integrated-worker ;;
    api-gateway/*) add api-gateway ;;

    scripts/deploy-prod.sh|scripts/compose-services-from-paths.sh|.github/workflows/deploy.yml)
      mark_all
      ;;
  esac
}

PATHS=""
if [[ $# -gt 0 ]]; then
  PATHS="$*"
else
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" ]] && continue
    PATHS="${PATHS} ${line}"
  done
fi

PATHS="$(trim "${PATHS}")"

if [[ -z "${PATHS}" ]]; then
  echo "all"
  exit 0
fi

# shellcheck disable=SC2086
for p in ${PATHS}; do
  classify "$p"
done

WANT_LIST="$(trim "${WANT_LIST}")"

ORDER="elasticsearch user-service interaction-service integrated-api integrated-worker api-gateway"
OUT=""
for s in ${ORDER}; do
  if has_want "$s"; then
    OUT="${OUT} ${s}"
  fi
done
OUT="$(trim "${OUT}")"

APP_COUNT=0
for s in ${ALL_APP_SERVICES}; do
  if has_want "$s"; then
    APP_COUNT=$((APP_COUNT + 1))
  fi
done
ALL_COUNT=0
for _ in ${ALL_APP_SERVICES}; do
  ALL_COUNT=$((ALL_COUNT + 1))
done

if [[ "${APP_COUNT}" -eq "${ALL_COUNT}" ]]; then
  if has_want elasticsearch; then
    echo "all elasticsearch"
  else
    echo "all"
  fi
  exit 0
fi

echo "${OUT}"
