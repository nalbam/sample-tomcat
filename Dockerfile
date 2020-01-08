# Dockerfile

FROM tomcat:8-jre8-alpine

# RUN apk add --no-cache bash curl

EXPOSE 8080
EXPOSE 8088

RUN rm -rf /usr/local/tomcat/webapps/*

COPY target/jmx/*.jar /jmx_javaagent.jar
COPY target/jmx/config.yaml /config.yaml

COPY target/*.war /usr/local/tomcat/webapps/ROOT.war
