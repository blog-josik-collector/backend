#!/usr/bin/env bash
#
# techblog-posts alias 기반 인덱스 관리 스크립트 (운영/수동 실행용)
#
# 매핑/세팅 소스:
#   common-elasticsearch/src/main/resources/elasticsearch/techblog-posts.json
#   + techblog-user-dictionary.txt / techblog-synonyms.txt (정의 JSON 과 같은 디렉터리)
# alias 는 techblog-posts, 물리 인덱스는 techblog-posts-<yyMMddHHmmss> 로 생성한다.
#
# 사용법:
#   ./manage-index.sh status                 # alias -> 물리 인덱스 매핑 조회
#   ./manage-index.sh bootstrap              # alias 없으면 물리 인덱스 + alias 생성
#   ./manage-index.sh reindex                # 새 인덱스 생성 -> reindex -> alias 원자적 스왑
#
# 환경변수:
#   ES_URL       (기본: http://localhost:9200)
#   ES_USER      (선택: basic auth 사용자)
#   ES_PASSWORD  (선택: basic auth 비밀번호)
#   ES_ALIAS     (기본: techblog-posts)
#
# 의존: curl, jq
set -euo pipefail

ES_URL="${ES_URL:-http://localhost:9200}"
ES_ALIAS="${ES_ALIAS:-techblog-posts}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFINITION_FILE="${DEFINITION_FILE:-$SCRIPT_DIR/../../../common-elasticsearch/src/main/resources/elasticsearch/techblog-posts.json}"
DEFINITION_DIR="$(cd "$(dirname "$DEFINITION_FILE")" && pwd)"
USER_DICT_FILE="${USER_DICT_FILE:-$DEFINITION_DIR/techblog-user-dictionary.txt}"
SYNONYMS_FILE="${SYNONYMS_FILE:-$DEFINITION_DIR/techblog-synonyms.txt}"

for bin in curl jq; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "[ERROR] '$bin' 가 필요합니다. 설치 후 다시 실행하세요." >&2
    exit 1
  fi
done

if [[ ! -f "$DEFINITION_FILE" ]]; then
  echo "[ERROR] 인덱스 정의 파일을 찾을 수 없습니다: $DEFINITION_FILE" >&2
  exit 1
fi
if [[ ! -f "$USER_DICT_FILE" ]]; then
  echo "[ERROR] 사용자 사전 파일을 찾을 수 없습니다: $USER_DICT_FILE" >&2
  exit 1
fi
if [[ ! -f "$SYNONYMS_FILE" ]]; then
  echo "[ERROR] 동의어 사전 파일을 찾을 수 없습니다: $SYNONYMS_FILE" >&2
  exit 1
fi

# curl 공통 옵션(-sS: 조용하지만 에러는 표시, --fail-with-body: 4xx/5xx 시 실패)
curl_es() {
  local method="$1"; shift
  local path="$1"; shift
  local auth=()
  if [[ -n "${ES_USER:-}" ]]; then
    auth=(-u "${ES_USER}:${ES_PASSWORD:-}")
  fi
  curl -sS --fail-with-body "${auth[@]}" \
    -X "$method" "${ES_URL}${path}" \
    -H 'Content-Type: application/json' "$@"
}

new_physical_index_name() {
  echo "${ES_ALIAS}-$(date +%y%m%d%H%M%S)"
}

# alias 가 현재 가리키는 물리 인덱스명 출력(없으면 빈 문자열)
current_index() {
  local resp
  if ! resp="$(curl_es GET "/_alias/${ES_ALIAS}" 2>/dev/null)"; then
    echo ""
    return 0
  fi
  # 다중 연결(비정상) 대비: 이름 정렬 후 최신(최근 timestamp) 선택
  echo "$resp" | jq -r 'keys | sort | last // empty'
}

# 주석/빈 줄을 제외한 사전 라인을 JSON 문자열 배열로 변환
lines_to_json_array() {
  local file="$1"
  grep -vE '^\s*(#|$)' "$file" \
    | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//' \
    | jq -R . \
    | jq -s .
}

# 정의 JSON 에 사용자 사전/동의어를 주입한 create-index body 를 stdout 으로 출력
assemble_definition() {
  local rules synonyms
  rules="$(lines_to_json_array "$USER_DICT_FILE")"
  synonyms="$(lines_to_json_array "$SYNONYMS_FILE")"
  if [[ "$(echo "$rules" | jq 'length')" -eq 0 ]]; then
    echo "[ERROR] 사용자 사전이 비어 있습니다: $USER_DICT_FILE" >&2
    exit 1
  fi
  if [[ "$(echo "$synonyms" | jq 'length')" -eq 0 ]]; then
    echo "[ERROR] 동의어 사전이 비어 있습니다: $SYNONYMS_FILE" >&2
    exit 1
  fi

  jq --argjson rules "$rules" --argjson synonyms "$synonyms" '
    .settings.analysis.tokenizer.nori_tech_tokenizer |=
      (del(.user_dictionary_rules_file) | .user_dictionary_rules = $rules)
    |
    .settings.analysis.filter.tech_synonym_filter |=
      (del(.synonyms_file) | .synonyms = $synonyms)
  ' "$DEFINITION_FILE"
}

create_physical_index() {
  local index="$1"
  echo "[INFO] 물리 인덱스 생성: ${index}"
  curl_es PUT "/${index}" --data-binary "$(assemble_definition)" >/dev/null
  echo "[INFO] 생성 완료: ${index}"
}

cmd_status() {
  echo "[INFO] alias=${ES_ALIAS} @ ${ES_URL}"
  if ! curl_es GET "/_alias/${ES_ALIAS}"; then
    echo "[INFO] alias 가 존재하지 않습니다."
  fi
  echo
}

cmd_bootstrap() {
  local existing
  existing="$(current_index)"
  if [[ -n "$existing" ]]; then
    echo "[INFO] 이미 존재합니다. alias=${ES_ALIAS} -> ${existing} (생성 생략)"
    return 0
  fi

  local index
  index="$(new_physical_index_name)"
  create_physical_index "$index"

  echo "[INFO] write alias 연결: ${ES_ALIAS} -> ${index}"
  curl_es POST "/_aliases" -d "$(cat <<EOF
{ "actions": [ { "add": { "index": "${index}", "alias": "${ES_ALIAS}", "is_write_index": true } } ] }
EOF
)" >/dev/null
  echo "[DONE] bootstrap 완료. alias=${ES_ALIAS} -> ${index}"
}

cmd_reindex() {
  local source
  source="$(current_index)"
  if [[ -z "$source" ]]; then
    echo "[ERROR] 재색인 원본이 없습니다. 먼저 bootstrap 을 실행하세요." >&2
    exit 1
  fi

  local dest
  dest="$(new_physical_index_name)"
  if [[ "$dest" == "$source" ]]; then
    echo "[ERROR] 새 인덱스명이 원본과 동일합니다(1초 내 재실행). 잠시 후 다시 시도하세요." >&2
    exit 1
  fi

  create_physical_index "$dest"

  echo "[INFO] 재색인: ${source} -> ${dest}"
  curl_es POST "/_reindex?refresh=true" -d "$(cat <<EOF
{ "source": { "index": "${source}" }, "dest": { "index": "${dest}" } }
EOF
)" >/dev/null

  echo "[INFO] alias 원자적 스왑: ${ES_ALIAS} ${source} -> ${dest}"
  curl_es POST "/_aliases" -d "$(cat <<EOF
{ "actions": [
  { "remove": { "index": "${source}", "alias": "${ES_ALIAS}" } },
  { "add": { "index": "${dest}", "alias": "${ES_ALIAS}", "is_write_index": true } }
] }
EOF
)" >/dev/null

  echo "[DONE] reindex 완료. alias=${ES_ALIAS} -> ${dest} (이전 ${source} 는 검증 후 수동 삭제)"
}

main() {
  local command="${1:-}"
  case "$command" in
    status)    cmd_status ;;
    bootstrap) cmd_bootstrap ;;
    reindex)   cmd_reindex ;;
    *)
      echo "사용법: $0 {status|bootstrap|reindex}" >&2
      exit 1
      ;;
  esac
}

main "$@"
