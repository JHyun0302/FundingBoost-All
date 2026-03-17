#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"

GUARD_CMD="cd ${PROJECT_DIR} && /usr/bin/env bash ${PROJECT_DIR}/deploy/oci/edge/scripts/traffic-guard.sh"
LOG_FILE="${PROJECT_DIR}/deploy/oci/edge/state/traffic-guard.log"
CRON_EXPR="${TRAFFIC_GUARD_CRON_EXPR:-*/5 * * * *}"
CRON_LINE="${CRON_EXPR} ${GUARD_CMD} >> ${LOG_FILE} 2>&1"

mkdir -p "${PROJECT_DIR}/deploy/oci/edge/state"

(
    crontab -l 2>/dev/null | grep -v 'deploy/oci/edge/scripts/traffic-guard.sh' || true
    echo "${CRON_LINE}"
) | crontab -

/usr/bin/env bash "${PROJECT_DIR}/deploy/oci/edge/scripts/traffic-guard.sh"
echo "installed cron: ${CRON_LINE}"
