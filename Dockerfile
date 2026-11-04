# syntax=docker/dockerfile:1

# build: compiles, tests and packages the WAR on the build platform.
FROM --platform=$BUILDPLATFORM maven:3.10.0-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B verify && mkdir /out && cp target/sample-tomcat-*.war /out/sample-tomcat.war

# artifact: the release artifact, exported by scripts/release.sh artifact.
FROM scratch AS artifact
COPY --from=build /out /

FROM tomcat:11.0.27-jre25-temurin

LABEL org.opencontainers.image.title="sample-tomcat" \
      org.opencontainers.image.description="A sample Spring MVC app on Tomcat for Kubernetes demos" \
      org.opencontainers.image.source="https://github.com/nalbam/sample-tomcat"

# Java agents, enabled by entrypoint.sh. Checksums are the published .sha256 files.
# ADD --chmod would also apply its mode to a missing /data, so create it first.
RUN install -d -m 755 /data
ADD --chmod=644 --checksum=sha256:b553475e2f00726cb8a7cd1c227dbe9236d4877869a85d57f4077cbc326332f4 \
    https://repo1.maven.org/maven2/com/datadoghq/dd-java-agent/1.68.0/dd-java-agent-1.68.0.jar \
    /data/dd-java-agent.jar
# Maven Central stops at JMX exporter 1.0.1; later releases are GitHub release assets.
ADD --chmod=644 --checksum=sha256:0ad843261c567d1924a017d542e14c40f2afff523d940c25ee000742fde4b3a9 \
    https://github.com/prometheus/jmx_exporter/releases/download/1.7.0/jmx_prometheus_javaagent-1.7.0.jar \
    /data/jmx_javaagent.jar

COPY jmx/config.yaml /data/config.yaml
COPY entrypoint.sh /data/entrypoint.sh

# The HTTP connector reads its port from the port.http property (entrypoint.sh).
# The non-root user owns webapps, where Tomcat expands the WAR, and conf/Catalina.
RUN sed -i '0,/<Connector port="8080"/s//<Connector port="${port.http}"/' conf/server.xml \
 && grep -q 'port="${port.http}"' conf/server.xml \
 && rm -rf webapps/* \
 && useradd --system --uid 10001 --no-create-home app \
 && mkdir -p conf/Catalina \
 && chown -R 10001 webapps conf/Catalina

COPY --from=artifact --chown=10001 /sample-tomcat.war webapps/ROOT.war

USER 10001

ARG VERSION=v0.0.0-dev
ENV PORT=8080 \
    VERSION=${VERSION}

# 8080: HTTP. 8081: JMX exporter when JAVA_AGENT_ENABLED=true.
EXPOSE 8080 8081

ENTRYPOINT ["/bin/sh", "/data/entrypoint.sh"]
