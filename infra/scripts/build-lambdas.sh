#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LAMBDA_DIR="${ROOT_DIR}/src/lambda"
SHARED_DIR="${LAMBDA_DIR}/shared"
ARTIFACT_DIR="${ROOT_DIR}/infra/artifacts"

mkdir -p "${ARTIFACT_DIR}"

build_lambda() {
  local name="$1"
  local src_dir="${LAMBDA_DIR}/${name}"
  local staging
  staging="$(mktemp -d)"

  cleanup() {
    rm -rf "${staging}"
  }
  trap cleanup RETURN

  if [[ ! -f "${src_dir}/index.js" ]]; then
    echo "ERROR: Missing ${src_dir}/index.js"
    exit 1
  fi

  cp "${src_dir}/index.js" "${staging}/"
  cp "${src_dir}/package.json" "${staging}/"
  cp -R "${SHARED_DIR}" "${staging}/shared"

  echo "Installing dependencies for ${name} ..."
  (cd "${staging}" && npm install --omit=dev --no-audit --no-fund)

  local zip_path="${ARTIFACT_DIR}/${name}.zip"
  rm -f "${zip_path}"
  (cd "${staging}" && zip -qr "${zip_path}" .)

  echo "Built ${zip_path} ($(du -h "${zip_path}" | cut -f1))"
}

echo "Building Lambda artifacts into ${ARTIFACT_DIR} ..."
build_lambda api-handler
build_lambda push-worker
build_lambda realtime-handler

echo "All Lambda artifacts built successfully."
