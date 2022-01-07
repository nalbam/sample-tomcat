# Dockerfile

FROM tomcat:8-jre8-alpine

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
COPY ./jmx/dd-java-agent-0.59.0.jar.zip /data/dd-java-agent.jar
COPY ./jmx/jmx_prometheus_javaagent-0.13.0.jar.zip /data/jmx_javaagent.jar

COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
