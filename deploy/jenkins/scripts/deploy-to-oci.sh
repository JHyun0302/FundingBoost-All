#!/usr/bin/env bash
set -euo pipefail

REF="${1:-unknown-ref}"
SHA="${2:-unknown-sha}"
REPOSITORY="${3:-unknown-repository}"
ACTOR="${4:-unknown-actor}"

echo "[deploy-to-oci] webhook payload received"
echo "[deploy-to-oci] ref=${REF}"
echo "[deploy-to-oci] sha=${SHA}"
echo "[deploy-to-oci] repository=${REPOSITORY}"
echo "[deploy-to-oci] actor=${ACTOR}"

echo "[deploy-to-oci] placeholder script"
echo "[deploy-to-oci] add your production deployment commands here."
echo "[deploy-to-oci] example:"
echo "  1) ./deploy/oci/scripts/build-arm-images.sh"
echo "  2) ssh funding-boost-front/back to upload artifacts"
echo "  3) docker load + docker compose up -d on each VM"
