#!/usr/bin/env bash
set -euo pipefail

JENKINS_URL="${JENKINS_URL:-http://localhost:8081}"
JOB_NAME="${JENKINS_JOB_NAME:-fundingboost-local-cd}"
TOKEN="${JENKINS_WEBHOOK_TOKEN:-local-fundingboost-token}"
REF="${REF:-refs/heads/main}"
SHA="${SHA:-local-$(date +%s)}"
REPOSITORY="${REPOSITORY:-local/FundingBoost}"
ACTOR="${ACTOR:-local-tester}"

curl -fsS -X POST \
  "${JENKINS_URL}/buildByToken/buildWithParameters" \
  --data-urlencode "job=${JOB_NAME}" \
  --data-urlencode "token=${TOKEN}" \
  --data-urlencode "ref=${REF}" \
  --data-urlencode "sha=${SHA}" \
  --data-urlencode "repository=${REPOSITORY}" \
  --data-urlencode "actor=${ACTOR}"

echo
echo "[trigger-local-webhook] triggered job=${JOB_NAME} ref=${REF} sha=${SHA}"
