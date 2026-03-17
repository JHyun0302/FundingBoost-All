#!/usr/bin/env bash
set -euo pipefail

REF="${1:-unknown-ref}"
SHA="${2:-unknown-sha}"
REPOSITORY="${3:-unknown-repository}"
ACTOR="${4:-unknown-actor}"

echo "[local-deploy] start"
echo "[local-deploy] ref=${REF}"
echo "[local-deploy] sha=${SHA}"
echo "[local-deploy] repository=${REPOSITORY}"
echo "[local-deploy] actor=${ACTOR}"

# Default mode is dry-run for safe local verification.
if [[ "${LOCAL_DEPLOY_DRY_RUN:-true}" == "true" ]]; then
  echo "[local-deploy] dry-run enabled. No real deployment executed."
  exit 0
fi

echo "[local-deploy] dry-run disabled. Running OCI deploy script..."
bash /workspace/fundingboost/deploy/jenkins/scripts/deploy-to-oci.sh "${REF}" "${SHA}" "${REPOSITORY}" "${ACTOR}"

echo "[local-deploy] done"
