# Dockerfile

FROM tomcat:11.0.27-jre25-temurin

LABEL maintainer="me@nalbam.com" \
      org.opencontainers.image.description="A Sample Docker image for Tomcat App" \
      org.opencontainers.image.authors="Jungyoul Yu, me@nalbam.com, https://www.nalbam.com/" \
      org.opencontainers.image.source="https://github.com/nalbam/sample-tomcat" \
      org.opencontainers.image.title="sample-tomcat"

# RUN apk add --no-cache bash curl

EXPOSE 8080
EXPOSE 8081

RUN rm -rf /usr/local/tomcat/webapps/*

ENTRYPOINT ["/bin/sh", "/data/entrypoint.sh"]

COPY ./entrypoint.sh /data/entrypoint.sh

COPY ./jmx/config.yaml /data/config.yaml
ADD https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/1.68.0/dd-java-agent-1.68.0.jar /data/dd-java-agent.jar
ADD https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.1/jmx_prometheus_javaagent-1.0.1.jar /data/jmx_javaagent.jar

COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
