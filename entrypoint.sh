#!/bin/bash

if [ "${JMX_ENABLED}" == "true" ]; then
    JMX_PATH=${JMX_PATH:-"/data/jmx_javaagent.jar"}
    JMX_PORT=${JMX_PORT:-8081}
    JMX_CONFIG=${JMX_CONFIG:-"/data/config.yaml"}

    JMX_OPTS="-javaagent:${JMX_PATH}=${JMX_PORT}:${JMX_CONFIG}"
fi

export JAVA_OPTS="${JAVA_OPTS} ${JMX_OPTS}"

${CATALINA_HOME}/bin/catalina.sh run
