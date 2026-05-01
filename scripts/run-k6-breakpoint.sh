#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
K6_IMAGE="${K6_IMAGE:-grafana/k6:0.49.0}"
START_STACK="${START_STACK:-true}"
RESET_STACK="${RESET_STACK:-false}"
RESULT_DIR="${RESULT_DIR:-target/k6-breakpoint/$(date +%Y%m%d-%H%M%S)}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to run the local breakpoint test."
  exit 1
fi

mkdir -p "${RESULT_DIR}"

if [ "${START_STACK}" = "true" ]; then
  if [ "${RESET_STACK}" = "true" ]; then
    docker compose down -v
  fi
  docker compose up -d --build
fi

echo "Waiting for ${BASE_URL}/actuator/health ..."
for attempt in $(seq 1 90); do
  if curl --fail --silent --show-error "${BASE_URL}/actuator/health" | grep -q '"status":"UP"'; then
    echo "Application is healthy."
    break
  fi

  if [ "${attempt}" = "90" ]; then
    echo "Application did not become healthy in time."
    docker compose ps || true
    docker compose logs backend || true
    exit 1
  fi

  sleep 2
done

echo "Running k6 breakpoint test."
echo "Results: ${RESULT_DIR}"

docker run --rm \
  --network host \
  -e BASE_URL="${BASE_URL}" \
  -e K6_CLEANUP_WRITES="${K6_CLEANUP_WRITES:-true}" \
  -e K6_BREAKPOINT_RATES="${K6_BREAKPOINT_RATES:-10,20,40,60,80,100,125,150}" \
  -e K6_STAGE_DURATION="${K6_STAGE_DURATION:-2m}" \
  -e K6_COOLDOWN_DURATION="${K6_COOLDOWN_DURATION:-1m}" \
  -e K6_PRE_ALLOCATED_VUS="${K6_PRE_ALLOCATED_VUS:-50}" \
  -e K6_MAX_VUS="${K6_MAX_VUS:-250}" \
  -v "${PWD}:/workspace" \
  -w /workspace \
  "${K6_IMAGE}" run performance/k6/appointmentscheduler-breakpoint.js

if [ -d target/k6 ]; then
  cp -R target/k6/. "${RESULT_DIR}/"
fi

docker compose logs > "${RESULT_DIR}/docker-compose.log" || true

cat <<EOF

Breakpoint test complete.

Key files:
  ${RESULT_DIR}/summary.json
  ${RESULT_DIR}/metrics.json
  ${RESULT_DIR}/checks.json
  ${RESULT_DIR}/http-status.json
  ${RESULT_DIR}/report.html
  ${RESULT_DIR}/docker-compose.log

Tune with environment variables, for example:
  K6_BREAKPOINT_RATES=20,40,80,120,160 K6_STAGE_DURATION=3m K6_MAX_VUS=300 scripts/run-k6-breakpoint.sh

EOF
