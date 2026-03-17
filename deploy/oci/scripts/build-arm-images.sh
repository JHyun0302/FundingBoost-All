#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
OUT_DIR="${1:-$ROOT_DIR/deploy/oci/artifacts}"

mkdir -p "$OUT_DIR"

echo "[1/6] Build front image (linux/arm64)"
docker buildx build \
  --platform linux/arm64 \
  -t fundingboost-front:latest \
  -f "$ROOT_DIR/FundingBoost-Client/Dockerfile.nginx" \
  "$ROOT_DIR/FundingBoost-Client" \
  --load

echo "[2/6] Build server image (linux/arm64)"
docker buildx build \
  --platform linux/arm64 \
  -t fundingboost-server:latest \
  -f "$ROOT_DIR/FundingBoost-Server/Dockerfile" \
  "$ROOT_DIR/FundingBoost-Server" \
  --load

echo "[3/6] Build crawler image (linux/arm64)"
docker buildx build \
  --platform linux/arm64 \
  -t fundingboost-crawler:latest \
  -f "$ROOT_DIR/FundingBoost-DataCrawler/Dockerfile" \
  "$ROOT_DIR/FundingBoost-DataCrawler" \
  --load

echo "[4/6] Save front image"
docker save fundingboost-front:latest | gzip > "$OUT_DIR/fundingboost-front_arm64.tar.gz"

echo "[5/6] Save server image"
docker save fundingboost-server:latest | gzip > "$OUT_DIR/fundingboost-server_arm64.tar.gz"

echo "[6/6] Save crawler image"
docker save fundingboost-crawler:latest | gzip > "$OUT_DIR/fundingboost-crawler_arm64.tar.gz"

echo "Done. Artifacts: $OUT_DIR"
