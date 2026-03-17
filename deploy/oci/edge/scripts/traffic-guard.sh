#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EDGE_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
OCI_DIR="$(cd "${EDGE_DIR}/.." && pwd)"

ENV_FILE="${OCI_DIR}/env/edge.env"
if [[ -f "${ENV_FILE}" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "${ENV_FILE}"
    set +a
fi

TRAFFIC_THRESHOLD_TB="${TRAFFIC_THRESHOLD_TB:-9}"
EDGE_CONTAINER_NAME="${EDGE_CONTAINER_NAME:-fundingboost-edge}"
TRAFFIC_INTERFACE="${TRAFFIC_INTERFACE:-}"
FRONT_OFFLOAD_ORIGIN="${FRONT_OFFLOAD_ORIGIN:-}"

INCLUDES_DIR="${EDGE_DIR}/includes"
MODE_NORMAL_FILE="${INCLUDES_DIR}/mode.normal.conf"
MODE_OFFLOAD_FILE="${INCLUDES_DIR}/mode.offload.conf"
MODE_THROTTLE_FILE="${INCLUDES_DIR}/mode.throttle.conf"
MODE_CURRENT_FILE="${INCLUDES_DIR}/mode.current.conf"
STATUS_FILE="${INCLUDES_DIR}/traffic-status.json"

STATE_DIR="${EDGE_DIR}/state"
STATE_FILE="${STATE_DIR}/traffic-guard.state"
LOCK_FILE="${STATE_DIR}/traffic-guard.lock"
LOG_FILE="${STATE_DIR}/traffic-guard.log"

mkdir -p "${STATE_DIR}"

if command -v flock >/dev/null 2>&1; then
    exec 9>"${LOCK_FILE}"
    flock -n 9 || exit 0
fi

log() {
    printf '[%s] %s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$*" >> "${LOG_FILE}"
}

to_bytes_from_tb() {
    awk -v tb="$1" 'BEGIN { printf "%.0f", tb * 1024 * 1024 * 1024 * 1024 }'
}

to_tb() {
    awk -v b="$1" 'BEGIN { printf "%.4f", b / 1099511627776 }'
}

to_ratio() {
    awk -v cur="$1" -v thr="$2" 'BEGIN { if (thr <= 0) printf "0.00"; else printf "%.2f", (cur / thr) * 100 }'
}

detect_interface() {
    local detected=""
    if command -v ip >/dev/null 2>&1; then
        detected="$(ip route get 1.1.1.1 2>/dev/null | awk '{for (i=1; i<=NF; i++) if ($i=="dev") {print $(i+1); exit}}')"
        if [[ -z "${detected}" ]]; then
            detected="$(ip -o link show 2>/dev/null | awk -F': ' '$2 != "lo" {print $2; exit}')"
        fi
    fi
    if [[ -z "${detected}" ]] && [[ -d /sys/class/net ]]; then
        detected="$(find /sys/class/net -mindepth 1 -maxdepth 1 -type l -exec basename {} \; | grep -v '^lo$' | head -n 1 || true)"
    fi
    printf '%s' "${detected}"
}

write_status() {
    local month="$1"
    local threshold_tb="$2"
    local threshold_bytes="$3"
    local monthly_bytes="$4"
    local mode="$5"
    local mode_reason="$6"
    local iface="$7"

    local monthly_tb
    local ratio
    local offload_configured="false"

    monthly_tb="$(to_tb "${monthly_bytes}")"
    ratio="$(to_ratio "${monthly_bytes}" "${threshold_bytes}")"
    if [[ -n "${FRONT_OFFLOAD_ORIGIN}" ]]; then
        offload_configured="true"
    fi

    cat > "${STATUS_FILE}" <<EOF
{
  "month": "${month}",
  "thresholdTb": ${threshold_tb},
  "thresholdBytes": ${threshold_bytes},
  "monthlyBytes": ${monthly_bytes},
  "monthlyTb": "${monthly_tb}",
  "thresholdRatio": "${ratio}",
  "mode": "${mode}",
  "modeReason": "${mode_reason}",
  "offloadConfigured": ${offload_configured},
  "interface": "${iface}",
  "updatedAt": "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
}
EOF
}

apply_mode() {
    local target="$1"
    local source=""

    case "${target}" in
        normal) source="${MODE_NORMAL_FILE}" ;;
        offload) source="${MODE_OFFLOAD_FILE}" ;;
        throttle) source="${MODE_THROTTLE_FILE}" ;;
        *)
            log "invalid mode target: ${target}"
            return 1
            ;;
    esac

    if [[ ! -f "${source}" ]]; then
        log "missing mode file: ${source}"
        return 1
    fi

    cp "${source}" "${MODE_CURRENT_FILE}"

    if docker ps --format '{{.Names}}' | grep -Fxq "${EDGE_CONTAINER_NAME}"; then
        if ! docker exec "${EDGE_CONTAINER_NAME}" nginx -t >/dev/null 2>&1; then
            log "nginx config test failed after switching to ${target}"
            return 1
        fi
        if ! docker exec "${EDGE_CONTAINER_NAME}" nginx -s reload >/dev/null 2>&1; then
            log "nginx reload failed after switching to ${target}"
            return 1
        fi
    else
        log "container ${EDGE_CONTAINER_NAME} is not running; mode file only updated"
    fi

    log "mode switched to ${target}"
    return 0
}

if [[ ! -f "${MODE_CURRENT_FILE}" ]]; then
    cp "${MODE_NORMAL_FILE}" "${MODE_CURRENT_FILE}"
fi

if [[ -z "${TRAFFIC_INTERFACE}" ]]; then
    TRAFFIC_INTERFACE="$(detect_interface)"
fi

if [[ -z "${TRAFFIC_INTERFACE}" ]]; then
    log "failed to detect traffic interface"
    exit 1
fi

TX_FILE="/sys/class/net/${TRAFFIC_INTERFACE}/statistics/tx_bytes"
if [[ ! -r "${TX_FILE}" ]]; then
    log "tx stats file not readable: ${TX_FILE}"
    exit 1
fi

THRESHOLD_BYTES="$(to_bytes_from_tb "${TRAFFIC_THRESHOLD_TB}")"
CURRENT_MONTH="$(date '+%Y-%m')"
CURRENT_TX_BYTES="$(cat "${TX_FILE}")"

MONTH=""
LAST_TX_BYTES=0
MONTHLY_BYTES=0
MODE="normal"
MODE_REASON="bootstrap"

if [[ -f "${STATE_FILE}" ]]; then
    # shellcheck disable=SC1090
    source "${STATE_FILE}"
fi

if [[ "${MONTH}" != "${CURRENT_MONTH}" ]]; then
    MONTH="${CURRENT_MONTH}"
    LAST_TX_BYTES="${CURRENT_TX_BYTES}"
    MONTHLY_BYTES=0
    MODE_REASON="month_reset"
fi

if (( CURRENT_TX_BYTES >= LAST_TX_BYTES )); then
    DELTA_BYTES=$((CURRENT_TX_BYTES - LAST_TX_BYTES))
    MONTHLY_BYTES=$((MONTHLY_BYTES + DELTA_BYTES))
    LAST_TX_BYTES="${CURRENT_TX_BYTES}"
else
    LAST_TX_BYTES="${CURRENT_TX_BYTES}"
    MODE_REASON="counter_reset"
    log "tx counter reset detected on interface ${TRAFFIC_INTERFACE}"
fi

TARGET_MODE="normal"
TARGET_REASON="below_threshold"

if (( MONTHLY_BYTES >= THRESHOLD_BYTES )); then
    if [[ -n "${FRONT_OFFLOAD_ORIGIN}" ]]; then
        TARGET_MODE="offload"
        TARGET_REASON="threshold_reached"
    else
        TARGET_MODE="throttle"
        TARGET_REASON="threshold_reached_no_front_offload_origin"
    fi
fi

if [[ "${MODE}" != "${TARGET_MODE}" ]]; then
    if apply_mode "${TARGET_MODE}"; then
        MODE="${TARGET_MODE}"
        MODE_REASON="${TARGET_REASON}"
    else
        MODE_REASON="mode_apply_failed"
    fi
else
    MODE_REASON="${TARGET_REASON}"
fi

cat > "${STATE_FILE}" <<EOF
MONTH=${MONTH}
LAST_TX_BYTES=${LAST_TX_BYTES}
MONTHLY_BYTES=${MONTHLY_BYTES}
MODE=${MODE}
MODE_REASON=${MODE_REASON}
EOF

write_status "${MONTH}" "${TRAFFIC_THRESHOLD_TB}" "${THRESHOLD_BYTES}" "${MONTHLY_BYTES}" "${MODE}" "${MODE_REASON}" "${TRAFFIC_INTERFACE}"
