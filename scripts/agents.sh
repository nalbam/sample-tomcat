#!/bin/bash
# Starts an image once per Java agent setting and checks each container:
# Tomcat starts, scripts/smoke.sh passes, each agent loads its own JAR, and the JMX exporter serves /metrics.
# Datadog gets no API key and sends only to localhost:8126 inside the container, where nothing listens.
# Uses host ports 8080, 8081 and 9404.
# The container name is unique to this run; only containers that this run created are removed.
# Usage: scripts/agents.sh <image>
IMAGE="${1:?usage: scripts/agents.sh <image>}"
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
export CONTAINER="sample-tomcat-agents-$$-${RANDOM}"
export EXPECT_VERSION=v0.0.0-agents
CID=""

TMP="$(mktemp -d)"
cleanup() {
  [ -n "${CID}" ] && docker rm -f "${CID}" > /dev/null 2>&1
  rm -rf "${TMP}"
}
trap cleanup EXIT

fail() {
  echo "AGENTS FAIL [${NAME}]: $1"
  [ -n "${CID}" ] && docker logs "${CID}" 2>&1 | tail -100
  exit 1
}

# start <name> [docker run options]: starts the container and runs the smoke test.
start() {
  NAME="$1"
  shift
  echo "--- ${NAME}"
  if [ -n "${CID}" ]; then
    docker rm -f "${CID}" > /dev/null || fail "docker rm failed"
    CID=""
  fi
  # docker writes the ID to the cidfile once it creates the container, also when the start fails.
  rm -f "${TMP}/cid"
  docker run --name "${CONTAINER}" --cidfile "${TMP}/cid" -d -p 8080:8080 -p 8081:8081 -p 9404:9404 \
    -e VERSION="${EXPECT_VERSION}" \
    -e DD_INSTRUMENTATION_TELEMETRY_ENABLED=false -e DD_REMOTE_CONFIGURATION_ENABLED=false \
    "$@" "${IMAGE}" > /dev/null
  local status=$?
  [ -s "${TMP}/cid" ] && CID="$(cat "${TMP}/cid")"
  [ "${status}" -eq 0 ] || fail "docker run failed"
  for _ in $(seq 1 60); do
    curl -fs http://localhost:8080/live > /dev/null && break
    [ "$(docker inspect -f '{{.State.Running}}' "${CONTAINER}")" = "true" ] || fail "container exited"
    sleep 2
  done
  curl -fs http://localhost:8080/live > /dev/null || fail "/live did not respond"
  "${SCRIPTS}/smoke.sh" http://localhost:8080 || exit 1
}

# expect_agents <-javaagent options>: Tomcat logs the JVM arguments at startup.
expect_agents() {
  local got
  got="$(docker logs "${CONTAINER}" 2>&1 | grep -o 'Command line argument: -javaagent:[^ ]*' | sed 's/^Command line argument: //' | xargs)"
  [ "${got}" = "$1" ] || fail "javaagent options: want '$1', got '${got}'"
}

expect_datadog() {
  docker logs "${CONTAINER}" 2>&1 | grep -qF 'DATADOG TRACER CONFIGURATION' || fail "Datadog tracer did not start"
}

# expect_metrics <port> <regex that must match> [regex that must not match]
expect_metrics() {
  local metrics
  metrics="$(curl -fs "http://localhost:$1/metrics")" || fail "GET :$1/metrics failed"
  echo "${metrics}" | grep -qE "$2" || fail ":$1/metrics has no line matching $2"
  if [ -n "$3" ] && echo "${metrics}" | grep -qE "$3"; then
    fail ":$1/metrics has a line matching $3"
  fi
}

expect_no_metrics() {
  ! curl -fs -o /dev/null "http://localhost:$1/metrics" || fail ":$1/metrics responds"
}

DD_JAR=/data/dd-java-agent.jar
JMX_JAR=/data/jmx_javaagent.jar

start "no agents"
expect_agents ""
expect_no_metrics 8081

start "datadog" -e DD_AGENT_ENABLED=true
expect_agents "-javaagent:${DD_JAR}"
expect_datadog
expect_no_metrics 8081

start "jmx" -e JAVA_AGENT_ENABLED=true
expect_agents "-javaagent:${JMX_JAR}=8081:/data/config.yaml"
expect_metrics 8081 '^catalina_globalrequestprocessor_requestcount\{'
expect_metrics 8081 '^jvm_memory_used_bytes\{'

start "datadog and jmx" -e DD_AGENT_ENABLED=true -e JAVA_AGENT_ENABLED=true
expect_agents "-javaagent:${DD_JAR} -javaagent:${JMX_JAR}=8081:/data/config.yaml"
expect_datadog
expect_metrics 8081 '^catalina_globalrequestprocessor_requestcount\{'

# Custom JMX path, port and config, next to Datadog.
NAME="custom jmx setup"
CP_ID="$(docker create "${IMAGE}")" || fail "docker create failed"
docker cp "${CP_ID}:${JMX_JAR}" "${TMP}/jmx.jar"
CP_STATUS=$?
docker rm "${CP_ID}" > /dev/null
[ "${CP_STATUS}" -eq 0 ] || fail "docker cp failed"
printf 'lowercaseOutputName: true\nincludeObjectNames: ["java.lang:type=Memory"]\nrules:\n  - pattern: ".*"\n' > "${TMP}/jmx.yaml"
chmod a+r "${TMP}/jmx.jar" "${TMP}/jmx.yaml"

start "datadog and custom jmx" -v "${TMP}:/opt/agents:ro" \
  -e DD_AGENT_ENABLED=true -e JAVA_AGENT_ENABLED=true \
  -e JAVA_AGENT_PATH=/opt/agents/jmx.jar -e JAVA_AGENT_PORT=9404 -e JAVA_AGENT_CONFIG=/opt/agents/jmx.yaml
expect_agents "-javaagent:${DD_JAR} -javaagent:/opt/agents/jmx.jar=9404:/opt/agents/jmx.yaml"
expect_datadog
expect_metrics 9404 '^java_lang_memory_heapmemoryusage_used ' '^catalina_'
expect_no_metrics 8081

echo "AGENTS OK"
