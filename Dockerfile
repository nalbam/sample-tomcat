# Dockerfile

FROM tomcat:8-jre8-alpine

RUN apk add --no-cache bash curl

EXPOSE 8080
EXPOSE 8081

RUN rm -rf /usr/local/tomcat/webapps/*

ENV JMX_OPTS "-javaagent:/data/jmx_javaagent.jar=8081:/data/config.yaml"

COPY target/jmx/*.jar /data/jmx_javaagent.jar
COPY target/jmx/config.yaml /data/config.yaml

COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
