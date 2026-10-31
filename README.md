# sample-tomcat

[![build](https://img.shields.io/github/actions/workflow/status/nalbam/sample-tomcat/push.yaml?label=build&style=for-the-badge&logo=github)](https://github.com/nalbam/sample-tomcat/actions/workflows/push.yaml)
[![release](https://img.shields.io/github/v/release/nalbam/sample-tomcat?style=for-the-badge&logo=github)](https://github.com/nalbam/sample-tomcat/releases)

<!-- [![CircleCI](https://circleci.com/gh/nalbam/sample-tomcat.svg?style=svg)](https://circleci.com/gh/nalbam/sample-tomcat) -->

[![Docker Image Version (latest by date)](https://img.shields.io/docker/v/nalbam/sample-tomcat?label=Docker%20Hub&style=for-the-badge&logo=docker)](https://hub.docker.com/r/nalbam/sample-tomcat)
[![Docker Image Size (latest by date)](https://img.shields.io/docker/image-size/nalbam/sample-tomcat?style=for-the-badge&logo=docker)](https://hub.docker.com/r/nalbam/sample-tomcat)
[![Docker Pulls](https://img.shields.io/docker/pulls/nalbam/sample-tomcat?style=for-the-badge&logo=docker)](https://hub.docker.com/r/nalbam/sample-tomcat)

<!-- [![DockerHub Badge](http://dockeri.co/image/nalbam/sample-tomcat)](https://hub.docker.com/r/nalbam/sample-tomcat/) -->

## Docker

The image is published to two registries: Docker Hub (`nalbam/sample-tomcat`) and GitHub Container Registry (`ghcr.io/nalbam/sample-tomcat`).

```bash
docker pull nalbam/sample-tomcat
```

```bash
docker pull ghcr.io/nalbam/sample-tomcat
```

```bash
docker run -p 8080:8080 nalbam/sample-tomcat
```

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
| JDK (compile, CI, image) | 25 | `pom.xml` `java.version`, `push.yaml`, `Dockerfile` |
| Maven | 3.10.0 | `push.yaml` (checksum verified), `Jenkinsfile` image `maven:3.10.0-eclipse-temurin-25` |
| Spring Framework | 7.0.10 | `pom.xml` |
| Datadog Java agent | 1.68.0 | `Dockerfile`, from Maven Central |
| Prometheus JMX exporter | 1.7.0 | `Dockerfile`, from the GitHub release (Maven Central stops at 1.0.1) |

Both agent JARs are pinned with `ADD --checksum=sha256:...` to the `.sha256` files that the releases publish. This needs BuildKit (the default builder since Docker 23).

Java 25 exception: JDK 27 is the latest release, but the official `tomcat` image has no JDK 27 variant of Tomcat 11.0.27 (only `jre17`, `jre21` and `jre25`).
The project stays on Java 25, the newest LTS that the official image supports, so the compile target, CI and the image run the same Java.

## Build and test

```bash
mvn package          # compiles, runs the tests and creates target/*.war
```

`./build.sh` runs the same `mvn package`.

```bash
mvn -B clean package
./scripts/test-entrypoint.sh                  # -javaagent options of entrypoint.sh, no Docker needed
./scripts/test-agents.sh                      # scripts/agents.sh removes only its own containers, no Docker needed
docker build -t sample-tomcat:local .         # needs target/*.war
docker run --name smoke -d -p 8080:8080 -e VERSION=v0.0.0-local sample-tomcat:local
EXPECT_VERSION=v0.0.0-local ./scripts/smoke.sh # wait until http://localhost:8080/live responds
docker rm -f smoke
./scripts/agents.sh sample-tomcat:local       # uses ports 8080, 8081 and 9404
```

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
| `/` | HTML page (JSP). Needs the JSTL `c` taglib (`jakarta.tags.core`). |
| `/live`, `/read`, `/health` | Health checks. Return JSON. |
| `/stress` | CPU load |
| `/delay/{sec}` | Responds after `sec` seconds |
| `/timeout/{sec}` | Waits `sec` seconds, then fails with `TimeoutException` |
| `/fault/{rate}` | Fails with probability `rate` percent |
| `/loop/{count}` | Calls itself `count` times |
| `/node` | Calls `sample-node` |

CI runs `scripts/smoke.sh` against the container after `/live` responds. It checks that `/` has no unprocessed `<c:` or `${` and that `/static/css/main.css` and `/static/js/main.js` are linked.
Every local `href`/`src` in the page (for example `/static/favicon.ico`) must be under `<context>/static/` and return 200.
It also checks the JSON of `/live` and `/read` (`{"result":"OK","type":"live"}`) and `/health` (`{"result":"OK","type":"health","version":...}`).
If `EXPECT_VERSION` is set, `/health` must report it as `version`.
For a non-ROOT deployment, pass the context in the URL: `scripts/smoke.sh http://localhost:8080/sample`.

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

## Release

Pull requests and pushes to `main` or `master` run the `build` check in [push.yaml](.github/workflows/push.yaml) on Temurin 25:
Maven 3.10.0, `./build.sh` (`mvn package` with the tests), `scripts/test-entrypoint.sh`, `scripts/test-agents.sh`, a local Docker build tagged `sample-tomcat:ci-<sha>`,
the smoke test above with `VERSION=ci-<sha>`, and `scripts/agents.sh` (see [Java agents](#java-agents)).
Branch pushes only run CI. They do not bump versions, create releases, push images or trigger GitOps.

To release, push a `v*` SemVer tag such as `v0.9.0` or `v0.10.0-rc.1`:

```bash
git tag v0.9.0
git push origin v0.9.0
```

Tags must match `vMAJOR.MINOR.PATCH` with an optional prerelease suffix.
Leading zeros, `+build` metadata and tags longer than 128 characters are rejected, so the Git tag and the image tag stay identical.

The [release workflow](.github/workflows/release.yml) runs these jobs in order. Each job needs the previous one to succeed.

1. Validates the tag.
2. Runs `push.yaml` as a reusable workflow. On tag runs it uploads the tested `target/*.war` as the Actions artifact `war-<sha>`. The upload fails if no WAR exists.
3. Checks out the same commit, downloads `war-<sha>` into `target/` and fails unless it holds exactly one WAR.
   It then builds `.` for `linux/amd64` and `linux/arm64` and pushes `<tag>` to `nalbam/sample-tomcat` and `ghcr.io/nalbam/sample-tomcat`.
   Stable tags also update `latest`. Prerelease tags only publish their version tag.
4. Creates a GitHub Release. The notes list the commits since the previous reachable `v*` tag, without merge commits and `chore: release` commits.
   The first release lists all commits. Tags with a prerelease suffix create GitHub prereleases. A rerun updates the existing release.

Release runs are serialized across tags, and a running release is not cancelled, because releases share `latest`.
The WAR is only published inside the images; it is not attached to the GitHub Release.
The release ends at the images and the GitHub Release. It does not send a GitOps dispatch, because `opspresso/argocd-env-demo` has no chart for this project yet.

Versions:

- The Git tag is the release version. It names the images and the GitHub Release.
- The Maven version in `pom.xml` is fixed (`v0.8.0`). It only names the WAR file, which the image renames to `ROOT.war`.
- The runtime `VERSION` (shown by the app) comes from the `VERSION` environment variable, default `v0.0.0`. The image does not set it; set it at deploy time.
- The legacy `VERSION` and `BUMP` files are not used by the release workflow.

Repository settings:

Check a release by hand. Tag only a commit whose `build` run succeeded:

```bash
gh run list -R nalbam/sample-tomcat -w build -b main -s success -L 5 --json headSha,url
```

After the `release` run of tag `v0.9.0` finishes:

```bash
TAG=v0.9.0
SHA=$(git rev-list -n 1 "${TAG}")
RUN=$(gh run list -R nalbam/sample-tomcat -w release -c "${SHA}" -L 1 --json databaseId -q '.[0].databaseId')
gh run view -R nalbam/sample-tomcat "${RUN}" --json conclusion,headSha,jobs -q '{conclusion, headSha, jobs: [.jobs[] | {name, conclusion}]}'

# The WAR artifact is named war-<SHA> of the tag commit (kept 7 days).
gh api "repos/nalbam/sample-tomcat/actions/runs/${RUN}/artifacts" -q '.artifacts[] | [.name, .expired]'
gh run download -R nalbam/sample-tomcat "${RUN}" -n "war-${SHA}" -D /tmp/war && sha256sum /tmp/war/*.war

# Both registries have the tag for linux/amd64 and linux/arm64.
docker buildx imagetools inspect "nalbam/sample-tomcat:${TAG}"
docker buildx imagetools inspect "ghcr.io/nalbam/sample-tomcat:${TAG}"

gh release view -R nalbam/sample-tomcat "${TAG}" --json tagName,isPrerelease,url

# The image holds the tested WAR as ROOT.war and runs the expected versions.
docker run --rm --entrypoint sha256sum "nalbam/sample-tomcat:${TAG}" /usr/local/tomcat/webapps/ROOT.war
docker run --rm --name check -d -p 8080:8080 -e VERSION="${TAG}" "nalbam/sample-tomcat:${TAG}"
for _ in $(seq 1 60); do curl -fs -o /dev/null http://localhost:8080/live && break; sleep 2; done
curl -fsS http://localhost:8080/live > /dev/null || echo "NOT READY: /live did not respond in 120 s"
curl -fsS http://localhost:8080/health                      # "version" is the VERSION value
docker logs check 2>&1 | grep -E 'Server version number|JVM Version'
docker stop check
```

The loop waits up to 120 seconds for `/live`. If Tomcat is not ready or a request fails, `curl -fsS` prints the error, for example `Connection refused` or `The requested URL returned error: 503`.
Read `docker logs check` before `docker stop check` removes the container.

The two `sha256sum` values must match. Run the same `docker run` checks with `ghcr.io/nalbam/sample-tomcat:${TAG}`.

- Secrets `DOCKER_USERNAME` and `DOCKER_PASSWORD` must allow pushing to `nalbam/sample-tomcat`.
- `GITHUB_TOKEN` pushes to `ghcr.io/nalbam/sample-tomcat`. The package must allow this repository write access.
- The default token permission is `contents: read`. Only the image job gets `packages: write` and only the GitHub Release job gets `contents: write`.

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
docker run -p 8080:8080 -p 8081:8081 -e JAVA_AGENT_ENABLED=true nalbam/sample-tomcat
curl http://localhost:8081/metrics
```

`scripts/agents.sh <image>` starts the image with no agents, Datadog only, JMX only, both, and both with a custom JMX path, port (`9404`) and configuration.
It names the container `sample-tomcat-agents-<pid>-<random>` and removes only the containers that it created, so an existing container is kept.
For each case it runs `scripts/smoke.sh`, compares the `-javaagent` options that Tomcat logs, and checks the metrics port.
Datadog gets no API key; it only tries `localhost:8126` inside the container, so the check sends no data to Datadog.
