#!/bin/bash
# Smoke test for a running container (default name "smoke", set CONTAINER to change it).
# GET / must render JSTL/EL, and the css and js links in the body must open.
# /live, /read and /health must return their JSON. If EXPECT_VERSION is set,
# /health must report it as "version" (the VERSION environment variable of the container).
# Usage: [CONTAINER=name] [EXPECT_VERSION=v1.2.3] scripts/smoke.sh [base_url]
BASE="${1:-http://localhost:8080}"

fail() {
  echo "SMOKE FAIL: $1"
  docker logs "${CONTAINER:-smoke}"
  exit 1
}

body=$(curl -fs "${BASE}/") || fail "GET / failed"

if echo "${body}" | grep -qF '<c:' || echo "${body}" | grep -qF '${'; then
  fail "GET / has unprocessed JSTL/EL"
fi

for path in /static/css/main.css /static/js/main.js; do
  echo "${body}" | grep -qF "\"${path}\"" || fail "link not found in body: ${path}"
  code=$(curl -s -o /dev/null -w '%{http_code}' "${BASE}${path}")
  [ "${code}" = "200" ] || fail "GET ${path} returned ${code}"
done

# check_json <path> <jq filter that must be true> [jq options]
check_json() {
  local path="$1" filter="$2" json
  shift 2
  json=$(curl -fs "${BASE}${path}") || fail "GET ${path} failed"
  echo "${json}" | jq -e "$@" "${filter}" > /dev/null || fail "GET ${path} returned ${json}, expected ${filter}"
}

# /read reports type "live"; this is the current controller contract.
check_json /live '.result == "OK" and .type == "live"'
check_json /read '.result == "OK" and .type == "live"'
check_json /health '.result == "OK" and .type == "health"'

if [ -n "${EXPECT_VERSION}" ]; then
  check_json /health '.version == $v' --arg v "${EXPECT_VERSION}"
fi

echo "SMOKE OK"
