#!/bin/bash
# Smoke test for a running container named "smoke".
# GET / must render JSTL/EL, and the css and js links in the body must open.
# Usage: scripts/smoke.sh [base_url]
BASE="${1:-http://localhost:8080}"

fail() {
  echo "SMOKE FAIL: $1"
  docker logs smoke
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

echo "SMOKE OK"
