# sample-tomcat

[![build](https://img.shields.io/github/actions/workflow/status/nalbam/sample-tomcat/push.yaml?label=build&style=for-the-badge&logo=github)](https://github.com/nalbam/sample-tomcat/actions/workflows/push.yaml)
[![release](https://img.shields.io/github/v/release/nalbam/sample-tomcat?style=for-the-badge&logo=github)](https://github.com/nalbam/sample-tomcat/releases)

<!-- [![CircleCI](https://circleci.com/gh/nalbam/sample-tomcat.svg?style=svg)](https://circleci.com/gh/nalbam/sample-tomcat) -->

[![Docker Image Version (latest by date)](https://img.shields.io/docker/v/nalbam/sample-tomcat?label=Docker%20Hub&style=for-the-badge&logo=docker)](https://hub.docker.com/r/nalbam/sample-tomcat)
[![Docker Image Size (latest by date)](https://img.shields.io/docker/image-size/nalbam/sample-tomcat?style=for-the-badge&logo=docker)](https://hub.docker.com/r/nalbam/sample-tomcat)
[![Docker Pulls](https://img.shields.io/docker/pulls/nalbam/sample-tomcat?style=for-the-badge&logo=docker)](https://hub.docker.com/r/nalbam/sample-tomcat)

<!-- [![DockerHub Badge](http://dockeri.co/image/nalbam/sample-tomcat)](https://hub.docker.com/r/nalbam/sample-tomcat/) -->

## Docker

```bash
docker pull nalbam/sample-tomcat
```

```bash
docker run -p 8080:8080 nalbam/sample-tomcat
```

## Requirements

- JDK 25 (LTS)
- Maven 3.9 or later
- Docker (to build the image)

The app is a Spring Framework 7 WAR (Jakarta EE 11). It runs on Tomcat 11 or later. Tomcat 9 and earlier do not work because they use the `javax.*` namespace.

## Build and test

```bash
mvn package          # compiles, runs the tests and creates target/*.war
```

`./build.sh` runs the same `mvn package`.

## Run

```bash
# Docker: build the image from target/*.war, then run it on http://localhost:8080
./build.sh
./build.sh start

# Jetty (development)
mvn jetty-ee11:run
```

To use another Tomcat 11 installation, copy `target/*.war` to `$CATALINA_HOME/webapps/ROOT.war`.

## Endpoints

| Path | Description |
|---|---|
| `/` | HTML page (JSP) |
| `/live`, `/read`, `/health` | Health checks. Return JSON. |
| `/stress` | CPU load |
| `/delay/{sec}` | Responds after `sec` seconds |
| `/timeout/{sec}` | Waits `sec` seconds, then fails with `TimeoutException` |
| `/fault/{rate}` | Fails with probability `rate` percent |
| `/loop/{count}` | Calls itself `count` times |
| `/node` | Calls `sample-node` |

## Java agents

`entrypoint.sh` starts the Java agents that the Dockerfile downloads from Maven Central.

| Variable | Description |
|---|---|
| `DD_AGENT_ENABLED=true` | Enables the Datadog agent |
| `JAVA_AGENT_ENABLED=true` | Enables the Prometheus JMX exporter on `JAVA_AGENT_PORT` (default `8081`) |
