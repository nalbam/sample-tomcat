#!/bin/bash
# Checks the -javaagent options that entrypoint.sh gives Tomcat, without Docker.
# A stub catalina.sh prints JAVA_OPTS instead of starting Tomcat.
# Usage: scripts/test-entrypoint.sh [entrypoint.sh]
ENTRYPOINT="$(cd "$(dirname "${1:-entrypoint.sh}")" && pwd)/$(basename "${1:-entrypoint.sh}")"

STUB="$(mktemp -d)"
trap 'rm -rf "${STUB}"' EXIT
mkdir -p "${STUB}/bin"
printf '#!/bin/sh\necho "${JAVA_OPTS}"\n' > "${STUB}/bin/catalina.sh"
chmod +x "${STUB}/bin/catalina.sh"

FAILED=0

# expect <name> <expected JAVA_OPTS> [VAR=value ...]
expect() {
  local name="$1" want="$2"
  shift 2
  local got
  got="$(env -i PATH="${PATH}" CATALINA_HOME="${STUB}" "$@" /bin/sh "${ENTRYPOINT}" | xargs)"
  if [ "${got}" = "${want}" ]; then
    echo "ok   ${name}"
  else
    echo "FAIL ${name}"
    echo "     want: ${want}"
    echo "     got:  ${got}"
    FAILED=1
  fi
}

expect "no agents" ""

expect "datadog" \
  "-javaagent:/data/dd-java-agent.jar" \
  DD_AGENT_ENABLED=true

expect "jmx" \
  "-javaagent:/data/jmx_javaagent.jar=8081:/data/config.yaml" \
  JAVA_AGENT_ENABLED=true

# Each agent must load its own JAR when both are enabled.
expect "datadog and jmx" \
  "-javaagent:/data/dd-java-agent.jar -javaagent:/data/jmx_javaagent.jar=8081:/data/config.yaml" \
  DD_AGENT_ENABLED=true JAVA_AGENT_ENABLED=true

expect "datadog and jmx with custom options" \
  "-Xmx256m -javaagent:/opt/dd.jar -javaagent:/opt/jmx.jar=9404:/opt/jmx.yaml" \
  JAVA_OPTS=-Xmx256m DD_AGENT_ENABLED=true DD_AGENT_PATH=/opt/dd.jar \
  JAVA_AGENT_ENABLED=true JAVA_AGENT_PATH=/opt/jmx.jar JAVA_AGENT_PORT=9404 JAVA_AGENT_CONFIG=/opt/jmx.yaml

# JAVA_AGENT_PATH is the JMX exporter JAR; it must not replace the Datadog JAR.
expect "datadog ignores JAVA_AGENT_PATH" \
  "-javaagent:/data/dd-java-agent.jar" \
  DD_AGENT_ENABLED=true JAVA_AGENT_PATH=/opt/jmx.jar

exit ${FAILED}
