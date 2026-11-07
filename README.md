# sample-tomcat

[![ci](https://img.shields.io/github/actions/workflow/status/nalbam/sample-tomcat/ci.yml?branch=main&label=ci&style=for-the-badge&logo=github)](https://github.com/nalbam/sample-tomcat/actions/workflows/ci.yml)
[![release](https://img.shields.io/github/v/release/nalbam/sample-tomcat?style=for-the-badge&logo=github)](https://github.com/nalbam/sample-tomcat/releases)

A sample Spring MVC WAR on Tomcat for Kubernetes demos, with optional Java agents.

## Docker

```bash
docker run -d --name sample-tomcat -p 8080:8080 ghcr.io/nalbam/sample-tomcat
until curl -fs http://localhost:8080/health; do sleep 1; done   # the JVM starts in a few seconds
docker rm -f sample-tomcat
```

The image runs Tomcat as the non-root uid `10001`. `entrypoint.sh` starts Tomcat with `exec`,
so Tomcat receives `SIGTERM` from `docker stop`.

| Environment variable | Default | Description |
| --- | --- | --- |
| `PORT` | `8080` | HTTP connector port (the `port.http` property in `conf/server.xml`). |
| `VERSION` | `v0.0.0-dev` | Version that `/health`, `/version` and `/` report. Release images set it to their tag; a local build sets it with `--build-arg VERSION=...`. |
| `JAVA_OPTS` | - | Options passed to `java`. |

## Requirements

- JDK 25 (LTS)
- Maven 3.10.0 or later
- Docker (to build the image)

`pom.xml` stops the build (`maven-enforcer-plugin`) on an older Maven or a JDK older than `java.version`.

The app is a Spring Framework 7 WAR (Jakarta EE 11). It runs on Tomcat 11 or later. Tomcat 9 and earlier do not work because they use the `javax.*` namespace.

### Versions

| Component | Version | Where |
|---|---|---|
| Tomcat image | `tomcat:11.0.27-jre25-temurin` | `Dockerfile` |
| JDK (compile, image) | 25 | `pom.xml` `java.version`, `Dockerfile` |
| Maven | 3.10.0 | `maven:3.10.0-eclipse-temurin-25` in the `Dockerfile` |
| Spring Framework | 7.0.10 | `pom.xml` |
| Datadog Java agent | 1.68.0 | `Dockerfile`, from Maven Central |
| Prometheus JMX exporter | 1.7.0 | `Dockerfile`, from the GitHub release (Maven Central stops at 1.0.1) |

Both agent JARs are pinned with `ADD --checksum=sha256:...` to the `.sha256` files that the releases publish. This needs BuildKit (the default builder since Docker 23).

Java 25 exception: JDK 27 is the latest release, but the official `tomcat` image has no JDK 27 variant of Tomcat 11.0.27 (only `jre17`, `jre21` and `jre25`).
The project stays on Java 25, the newest LTS that the official image supports, so the compile target, CI and the image run the same Java.

## Build and test

To build the WAR, run the tests in the image build stage, and smoke-test the image
(needs Docker with Buildx and `jq`):

```bash
docker buildx build --load -t sample-tomcat:dev .
scripts/smoke.sh sample-tomcat:dev
```

`RestfulControllerTests` loads `WEB-INF/appconfig-root.xml` with MockMvc (`spring-test`, no container). It checks the `/health` and `/version` JSON with a set and an unset version, `/live`, `/read`,
and `/ready` before and after shutdown: after a `ContextClosedEvent`, and after `close()` of the application context, `/ready` returns 503 and `/live` still returns 200.
`scripts/smoke.sh` checks `/health`, `/live`, `/ready`, `/version`, `/read`, that `/` renders the JSP and that its local assets open,
the non-root user, `docker stop`, each Java agent setting, and a custom `PORT`.

Without Docker, use JDK 25 and Maven 3.10.0 or later:

```bash
mvn -B verify            # compiles, runs the tests and creates target/*.war
mvn jetty-ee11:run       # development server
```

To use another Tomcat 11 installation, copy `target/*.war` to `$CATALINA_HOME/webapps/ROOT.war`.

## Endpoints

| Path | Description |
|---|---|
| `/` | HTML page (JSP). Needs the JSTL `c` taglib (`jakarta.tags.core`). |
| `/live` | Liveness probe. HTTP 200 `{"live":true}`, also during shutdown. |
| `/ready` | Readiness probe. HTTP 200 `{"ready":true}`. When shutdown starts (`SIGTERM` stops the web application and closes the Spring context), HTTP 503 `{"ready":false}`. |
| `/version` | HTTP 200 `{"version":"<VERSION>","java":"<java.version>"}`. `version` is the same value as in `/health`. |
| `/read` | HTTP 200 `{"result":"OK","type":"live"}` (kept from earlier versions) |
| `/health` | Health check. Returns JSON, see [Health response](#health-response). |
| `/stress` | CPU load |
| `/delay/{sec}` | Responds after `sec` seconds |
| `/timeout/{sec}` | Waits `sec` seconds, then fails with `TimeoutException` |
| `/fault/{rate}` | Fails with probability `rate` percent |
| `/loop/{count}` | Calls itself `count` times |
| `/node` | Calls `sample-node` |

### Health response

`/health` uses the same fields as `sample-node` and `sample-spring`:

| Field | Type | Value |
|---|---|---|
| `result` | string | `ok` or `error`. This app has no failing dependency check, so it returns `ok` whenever it responds. |
| `type` | string | `health` (kept from earlier versions) |
| `version` | string | `VERSION` environment variable (or the `version` system property), default `v0.0.0` |
| `uptime` | integer | Whole seconds since the JVM started (0 or more). It counts from the Tomcat process start, not from the WAR deployment. |

```bash
$ curl -s http://localhost:8080/health
{"result":"ok","type":"health","version":"v0.0.0","uptime":42}
```

The field order may differ. The probes use the same contract as `sample-node` and `sample-spring`:

```bash
$ curl -s http://localhost:8080/live
{"live":true}
$ curl -s http://localhost:8080/ready
{"ready":true}
$ curl -s http://localhost:8080/version
{"version":"v0.0.0","java":"25.0.1"}
```

`/read` keeps its own contract, `{"result":"OK","type":"live"}`.

CI runs `scripts/smoke.sh IMAGE VERSION` against the container after `/health` responds. `VERSION` defaults to `v0.0.0-smoke` and is passed to the container.
It checks that `/health` returns `result` `ok` and that `VERSION`, that `/live` has `live` true, `/ready` has `ready` true, `/version` has `version` equal to `VERSION`, and that `/read` contains `"type":"live"`.
It checks that `/` has no unprocessed `<c:` or `${`, shows `VERSION`, and that every local `href`/`src` in the page (for example `/static/css/main.css`) returns 200.
The smoke test does not check the 503 of `/ready` after `SIGTERM`, because Tomcat stops its connector first and takes no new request; `RestfulControllerTests` checks it.
For a non-ROOT deployment, deploy the WAR under another context path and open `http://localhost:8080/<context>/`. `scripts/smoke.sh` checks the image, which serves the app as ROOT.

## Page assets

`/` (`src/main/webapp/WEB-INF/views/index.jsp`) loads these browser libraries from CDNs, pinned with `integrity` (SRI) hashes:

| Library | Version | URL |
|---|---|---|
| Bootstrap CSS and JS | 5.3.8 | `cdn.jsdelivr.net/npm/bootstrap@5.3.8` |
| jQuery | 4.0.0 | `code.jquery.com/jquery-4.0.0.min.js` |
| Font Awesome Free | 7.3.1 | `use.fontawesome.com/releases/v7.3.1/css/all.css` |

Local assets (`/static/...`) use `${contextPath}`, so they work in ROOT and in other contexts.
No local script uses jQuery or Bootstrap JS. They stay because the external counter script below is outside this repository and its dependencies are unknown.
To change a version, update the URL and the `integrity` hash together, for example `curl -s <url> | openssl dgst -sha384 -binary | base64`.

Known constraint: the page also loads `https://nalbam.com/static/sample/counter.js`. That URL returns 404, and the counter API in `sample-node` was removed.
The thumbs up/down buttons therefore show `0` and do nothing. This is not a pass.

Browser check (needs Node.js and Playwright with Chromium):

```bash
npm i --prefix /tmp/pw playwright && npx --prefix /tmp/pw playwright install chromium
NODE_PATH=/tmp/pw/node_modules node scripts/browser-check.cjs http://localhost:8080/
```

It opens `/` at 1280x800 and 375x667 and saves screenshots in `target/browser/`.
It fails on HTTP, console or page errors, unrendered JSTL/EL, missing icons, unstyled buttons, horizontal overflow and local assets outside the context path.
It reports the `counter.js` failure as `known failure`, not as an error.
Add text arguments to require template values, for example `node scripts/browser-check.cjs <url> v1.2.3`.

## Java agents

`entrypoint.sh` adds a `-javaagent` option to `JAVA_OPTS` for each enabled agent. The agents can run alone or together.

| Variable | Default | Description |
|---|---|---|
| `DD_AGENT_ENABLED` | | `true` enables the Datadog agent |
| `DD_AGENT_PATH` | `/data/dd-java-agent.jar` | Datadog agent JAR |
| `JAVA_AGENT_ENABLED` | | `true` enables the Prometheus JMX exporter |
| `JAVA_AGENT_PATH` | `/data/jmx_javaagent.jar` | JMX exporter JAR |
| `JAVA_AGENT_PORT` | `8081` | JMX exporter port. Metrics are at `http://<host>:<port>/metrics`. |
| `JAVA_AGENT_CONFIG` | `/data/config.yaml` ([jmx/config.yaml](jmx/config.yaml)) | JMX exporter configuration |

`JAVA_AGENT_PATH` only sets the JMX exporter JAR. Use `DD_AGENT_PATH` for a custom Datadog JAR.
Configure the Datadog agent itself with its own `DD_*` variables, for example `DD_AGENT_HOST`.

```bash
docker run -p 8080:8080 -p 8081:8081 -e JAVA_AGENT_ENABLED=true ghcr.io/nalbam/sample-tomcat
curl http://localhost:8081/metrics
```

`scripts/smoke.sh` starts the image with no agent, Datadog only, JMX only, and both with a `JAVA_OPTS` value.
Datadog gets no API key; it only tries `localhost:8126` inside the container, so the check sends no data to Datadog.

## CI/CD

Pull requests and pushes to `main` run CI. A `v*` tag releases the image `ghcr.io/nalbam/sample-tomcat:<tag>`
and a GitHub Release with the WAR. Jenkins can run the same pipeline with Nexus and Harbor or ECR.
See [docs/pipeline.md](docs/pipeline.md).
