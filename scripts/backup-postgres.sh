#!/usr/bin/env bash
# 상용 VM용 Postgres 논리 백업 (compose 밖 — CI/CD 배포와 분리).
#
# 사전 조건:
#   - 저장소 루트에 .env (POSTGRES_*)
#   - docker compose -f docker-compose.prod.yml 로 postgres 가 떠 있음
#
# 사용 (저장소 루트 또는 이 스크립트 경로 기준):
#   ./scripts/backup-postgres.sh
#
# cron 예 (매일 03:15 KST, 로그는 호스트에):
#   15 3 * * * cd /opt/backend && ./scripts/backup-postgres.sh >> /var/log/backend-postgres-backup.log 2>&1
#
# 원격(R2/S3) 업로드는 이 스크립트 다음에 rclone/aws sync 를 붙이면 된다.
# 복구 예:
#   gunzip -c data/backups/postgres/postgres_YYYYMMDD_HHMMSS.sql.gz \
#     | docker compose -f docker-compose.prod.yml exec -T postgres \
#         psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
BACKUP_DIR="${BACKUP_DIR:-${ROOT_DIR}/data/backups/postgres}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"

if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

: "${POSTGRES_DB:?POSTGRES_DB is required (set in .env)}"
: "${POSTGRES_USER:?POSTGRES_USER is required (set in .env)}"

mkdir -p "${BACKUP_DIR}"

STAMP="$(date +%Y%m%d_%H%M%S)"
FILE="${BACKUP_DIR}/postgres_${STAMP}.sql.gz"
TMP="${FILE}.tmp"

echo "[backup-postgres] dumping ${POSTGRES_DB} → ${FILE}"

docker compose -f "${COMPOSE_FILE}" exec -T postgres \
  pg_dump -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" --no-owner --format=plain \
  | gzip > "${TMP}"

mv "${TMP}" "${FILE}"
echo "[backup-postgres] ok ${FILE} ($(du -h "${FILE}" | awk '{print $1}'))"

find "${BACKUP_DIR}" -type f -name 'postgres_*.sql.gz' -mtime "+${RETENTION_DAYS}" -delete
echo "[backup-postgres] retention: kept last ${RETENTION_DAYS} days under ${BACKUP_DIR}"
