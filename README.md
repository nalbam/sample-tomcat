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
| `/` | HTML page (JSP). Needs the JSTL `c` taglib (`jakarta.tags.core`). |
| `/live`, `/read`, `/health` | Health checks. Return JSON. |
| `/stress` | CPU load |
| `/delay/{sec}` | Responds after `sec` seconds |
| `/timeout/{sec}` | Waits `sec` seconds, then fails with `TimeoutException` |
| `/fault/{rate}` | Fails with probability `rate` percent |
| `/loop/{count}` | Calls itself `count` times |
| `/node` | Calls `sample-node` |

CI runs `scripts/smoke.sh` against the container after `/live` responds. It checks that `/` has no unprocessed `<c:` or `${` and that `/static/css/main.css` and `/static/js/main.js` return 200.

## Release

Pull requests and pushes to `main` or `master` run the `build` check in [push.yaml](.github/workflows/push.yaml) on Temurin 25:
`./build.sh` (`mvn package` with the tests), a local Docker build tagged `sample-tomcat:ci-<sha>`, and the smoke test above.
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

- Secrets `DOCKER_USERNAME` and `DOCKER_PASSWORD` must allow pushing to `nalbam/sample-tomcat`.
- `GITHUB_TOKEN` pushes to `ghcr.io/nalbam/sample-tomcat`. The package must allow this repository write access.
- The default token permission is `contents: read`. Only the image job gets `packages: write` and only the GitHub Release job gets `contents: write`.

## Java agents

`entrypoint.sh` starts the Java agents that the Dockerfile downloads from Maven Central.

| Variable | Description |
|---|---|
| `DD_AGENT_ENABLED=true` | Enables the Datadog agent |
| `JAVA_AGENT_ENABLED=true` | Enables the Prometheus JMX exporter on `JAVA_AGENT_PORT` (default `8081`) |
