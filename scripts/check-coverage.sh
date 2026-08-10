#!/usr/bin/env bash
# Jacoco LINE 커버리지가 모듈별 80% 이상인지 확인한다.
# 측정 범위: Service 계층부터 (repository / dto / config / Q* / Application 제외)
#
# 사용법 (저장소 루트):
#   ./scripts/check-coverage.sh                 # 4개 서비스 전부
#   ./scripts/check-coverage.sh user-service    # 특정 모듈만
#   ./scripts/check-coverage.sh user-service integrated-api
#
# 종료 코드: 모두 통과하면 0, 하나라도 미달/실패면 1

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

DEFAULT_MODULES=(
  user-service
  integrated-api
  integrated-worker
  interaction-service
)

MINIMUM_LINE_RATIO="${COVERAGE_MIN:-0.80}"

if [[ $# -gt 0 ]]; then
  MODULES=("$@")
else
  MODULES=("${DEFAULT_MODULES[@]}")
fi

line_coverage_pct() {
  local module="$1"
  local csv="${ROOT_DIR}/${module}/build/reports/jacoco/test/jacocoTestReport.csv"
  if [[ ! -f "${csv}" ]]; then
    echo "n/a"
    return
  fi
  python3 - "${csv}" <<'PY'
import csv, sys
path = sys.argv[1]
rows = list(csv.DictReader(open(path)))
missed = sum(int(r["LINE_MISSED"]) for r in rows)
covered = sum(int(r["LINE_COVERED"]) for r in rows)
total = missed + covered
if total == 0:
    print("n/a")
else:
    print(f"{100.0 * covered / total:.1f}%")
PY
}

echo "Jacoco LINE coverage check (minimum ${MINIMUM_LINE_RATIO} = $(python3 -c "print(f'{float(\"${MINIMUM_LINE_RATIO}\")*100:.0f}%')"))"
echo "Scope: Service~ (excludes **/repository/**, dto, config, Q*, *Application)"
echo

failed=0
declare -a summary=()

for module in "${MODULES[@]}"; do
  echo "=== ${module} ==="
  if ! ./gradlew \
      ":${module}:test" \
      ":${module}:jacocoTestReport"; then
    echo "FAIL  ${module}: test/report failed"
    summary+=("FAIL  ${module}  (test/report)")
    failed=1
    echo
    continue
  fi

  pct="$(line_coverage_pct "${module}")"
  html="${module}/build/reports/jacoco/test/html/index.html"

  if ./gradlew ":${module}:jacocoTestCoverageVerification"; then
    echo "PASS  ${module}: LINE ${pct}  (report: ${html})"
    summary+=("PASS  ${module}  LINE ${pct}")
  else
    echo "FAIL  ${module}: LINE ${pct} < $(python3 -c "print(f'{float(\"${MINIMUM_LINE_RATIO}\")*100:.0f}%')")  (report: ${html})"
    summary+=("FAIL  ${module}  LINE ${pct}")
    failed=1
  fi
  echo
done

echo "---------- summary ----------"
for line in "${summary[@]}"; do
  echo "${line}"
done

if [[ "${failed}" -ne 0 ]]; then
  echo
  echo "Coverage gate failed."
  exit 1
fi

echo
echo "All modules passed coverage gate."
exit 0
