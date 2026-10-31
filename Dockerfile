# syntax=docker/dockerfile:1
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
# Checksums are the published .sha256 files of each release.
ADD --checksum=sha256:b553475e2f00726cb8a7cd1c227dbe9236d4877869a85d57f4077cbc326332f4 \
    https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/1.68.0/dd-java-agent-1.68.0.jar /data/dd-java-agent.jar
# Maven Central stops at JMX exporter 1.0.1; later releases are GitHub release assets.
ADD --checksum=sha256:0ad843261c567d1924a017d542e14c40f2afff523d940c25ee000742fde4b3a9 \
    https://github.com/prometheus/jmx_exporter/releases/download/1.7.0/jmx_prometheus_javaagent-1.7.0.jar /data/jmx_javaagent.jar

COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
