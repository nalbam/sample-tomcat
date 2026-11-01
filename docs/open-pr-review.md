# Open pull request review

This page lists the open Dependabot pull requests of `nalbam/sample-tomcat` and shows why each one can be closed.
Every PR changes one `<version>` in `pom.xml`. The current `pom.xml` already uses the same or a newer artifact, and some PRs target an artifact that the project no longer uses.

This page records the review. It does not merge or close a PR. A maintainer closes the PRs.

## Review baseline

| Item | Value |
|---|---|
| `main` commit | [`26e4ee7168834911d017de9707c4204071102b86`](https://github.com/nalbam/sample-tomcat/commit/26e4ee7168834911d017de9707c4204071102b86) |
| PR list fetched | 2026-11-01T07:43:08Z (UTC) |
| Query | `gh api --paginate 'repos/nalbam/sample-tomcat/pulls?state=open&per_page=100'` (succeeded, 10 open PRs) |
| Read for each PR | body, diff (`gh pr diff`), changed files, `mergeable`, status checks |
| Maven check | Temurin JDK 25.0.5, Apache Maven 3.10.0 (SHA-512 from `push.yaml`), at `26e4ee7` |
| `build` CI at `26e4ee7` | [run 41203419765](https://github.com/nalbam/sample-tomcat/actions/runs/41203419765), `push` to `main`, success |

What all 10 PRs have in common:

- Author `dependabot[bot]`, base `main`, created 2022-12 to 2024-04.
- Each changes only `pom.xml`, one `<version>` line.
- `mergeable` is `CONFLICTING`: the lines they change no longer exist in that form.
- Their only checks are `CodeRabbit` and `Summary` (both `SUCCESS`). The `build` workflow did not run on them, so no PR has a build or test result.

The PR list changes over time. Before you close PRs, fetch it again (see [Check again](#check-again)).

## Replacing commits

| Commit | What it changed for these PRs |
|---|---|
| [`6d9d119`](https://github.com/nalbam/sample-tomcat/commit/6d9d11960898468cd915ed090f1fa041ab4b7589) (#181) | Moved to Java 25, Jakarta EE 11 and Spring 7. Set every version below, replaced HttpClient 4 with HttpClient 5, Jetty 11 with the Jetty 12 EE11 plugin, the JUnit API/engine pair with `junit-jupiter`, and Jackson 2 with Jackson 3 in the code. |
| [`5c287dd`](https://github.com/nalbam/sample-tomcat/commit/5c287dd0ec7b3bdcb439b68699e3e0a1938e48c6) (#185) | Added `maven-enforcer-plugin`: the build stops on Maven older than 3.10.0 or JDK older than `java.version` (25). Updated `slf4j-api` to 2.0.20. |

## Pull requests

"Proposed" is the change in the PR diff. "Current" is `pom.xml` at `26e4ee7`, confirmed by `mvn -B help:effective-pom` and `mvn -B dependency:tree`.

| PR | Proposed | Current at `26e4ee7` | Evidence | Why it can be closed |
|---|---|---|---|---|
| [#180](https://github.com/nalbam/sample-tomcat/pull/180) | `ch.qos.logback:logback-classic` 1.2.11 → 1.5.6 | `ch.qos.logback:logback-classic` 1.6.6 | `pom.xml`, `6d9d119`; tree: `logback-classic:1.6.6`, `logback-core:1.6.6` | Current version is newer than the proposal. |
| [#178](https://github.com/nalbam/sample-tomcat/pull/178) | `org.springframework:spring-webmvc` 5.3.19 → 6.1.6 | `org.springframework:spring-webmvc` 7.0.10 | `pom.xml`, `6d9d119`; tree: all `org.springframework` modules 7.0.10 | Current version is newer. Spring 7 is the Jakarta EE 11 baseline of this WAR; 6.1.6 would be a downgrade. |
| [#176](https://github.com/nalbam/sample-tomcat/pull/176) | `org.projectlombok:lombok` 1.18.24 → 1.18.32 | `lombok.version` = 1.18.48 (dependency and compiler `annotationProcessorPaths`) | `pom.xml`, `6d9d119`; tree: `lombok:1.18.48:provided` | Current version is newer. The version is now a property, so the PR's line no longer exists. |
| [#175](https://github.com/nalbam/sample-tomcat/pull/175) | `org.apache.maven.plugins:maven-compiler-plugin` 3.10.1 → 3.13.0 | `maven-compiler-plugin` 3.16.0 (`<release>25</release>`) | `pom.xml`, `6d9d119`; effective POM: 3.16.0 | Current version is newer. |
| [#173](https://github.com/nalbam/sample-tomcat/pull/173) | `com.fasterxml.jackson.core:jackson-databind` 2.13.2.2 → 2.17.0 | `com.fasterxml.jackson.core:jackson-databind` 2.22.4 (for `logback-jackson`), plus `tools.jackson.core:jackson-databind` 3.2.4 (for the code) | `pom.xml`, `RestfulController.java`, `logback.xml`, `6d9d119` (see [Jackson 2 and 3](#jackson-2-and-3)) | Current Jackson 2 version is newer. Keep Jackson 2: `logback-jackson` needs it. |
| [#166](https://github.com/nalbam/sample-tomcat/pull/166) | `org.junit.jupiter:junit-jupiter-api` 5.8.2 → 5.10.2 | `org.junit.jupiter:junit-jupiter` 6.1.3 (test) | `pom.xml`, `src/test/java/com/nalbam/webmvc/util/`, `6d9d119` (see [JUnit](#junit)) | The separate `junit-jupiter-api` dependency was removed. The aggregate brings `junit-jupiter-api` 6.1.3. |
| [#165](https://github.com/nalbam/sample-tomcat/pull/165) | `org.junit.jupiter:junit-jupiter-engine` 5.9.2 → 5.10.2 | `org.junit.jupiter:junit-jupiter` 6.1.3 (test) | `pom.xml`, `6d9d119` (see [JUnit](#junit)) | The separate `junit-jupiter-engine` dependency was removed. The aggregate brings `junit-jupiter-engine` 6.1.3. |
| [#164](https://github.com/nalbam/sample-tomcat/pull/164) | `org.eclipse.jetty:jetty-maven-plugin` 11.0.9 → 11.0.20 | `org.eclipse.jetty.ee11:jetty-ee11-maven-plugin` 12.1.14 | `pom.xml`, README `mvn jetty-ee11:run`, `6d9d119` (see [Jetty](#jetty)) | Different artifact. Jetty 11 supports only Jakarta EE 9 (Servlet 5) and cannot run this Servlet 6.1 WAR. |
| [#155](https://github.com/nalbam/sample-tomcat/pull/155) | `ch.qos.logback:logback-classic` 1.2.11 → 1.2.13 | `ch.qos.logback:logback-classic` 1.6.6 | `pom.xml`, `6d9d119` | Current version is newer. Also superseded by #180. |
| [#143](https://github.com/nalbam/sample-tomcat/pull/143) | `org.apache.httpcomponents:httpclient` 4.5.13 → 4.5.14 | `org.apache.httpcomponents.client5:httpclient5` 5.6.5 | `pom.xml`, `RestfulController.java`, `6d9d119` (see [HttpClient](#httpclient)) | Different artifact. HttpClient 4 was removed and the caller uses HttpClient 5. Spring 7 `HttpComponentsClientHttpRequestFactory` supports only HttpClient 5. |

## Evidence

### HttpClient

- `pom.xml` declares only `org.apache.httpcomponents.client5:httpclient5` 5.6.5.
- `src/main/java/com/nalbam/webmvc/controller/RestfulController.java` imports `org.apache.hc.client5.http.*` and `org.apache.hc.core5.util.Timeout`.
  `getRestTemplate()` builds a `CloseableHttpClient` with `PoolingHttpClientConnectionManagerBuilder` and passes it to `HttpComponentsClientHttpRequestFactory`. `/loop/{count}` and `/node` use it.
- `6d9d119` removed the `org.apache.http.client.HttpClient` and `org.apache.http.impl.client.HttpClientBuilder` imports.
- `dependency:tree`: `httpclient5:5.6.5` with `httpcore5:5.4.4` and `httpcore5-h2:5.4.4`. No `org.apache.httpcomponents:httpclient` is resolved, and the WAR has no HttpClient 4 JAR.

### Jetty

- `pom.xml` declares `org.eclipse.jetty.ee11:jetty-ee11-maven-plugin` 12.1.14. The effective POM has no `org.eclipse.jetty:jetty-maven-plugin`.
- The README runs Jetty with `mvn jetty-ee11:run`. The prefix `jetty-ee11` comes from the new plugin.
- The app uses `jakarta.servlet:jakarta.servlet-api` 6.1.0 (Jakarta EE 11).

### JUnit

- `pom.xml` declares one test dependency, `org.junit.jupiter:junit-jupiter` 6.1.3.
- `dependency:tree` resolves `junit-jupiter-api`, `junit-jupiter-params` and `junit-jupiter-engine` 6.1.3, with `junit-platform-engine` 6.1.3.
- The tests in `src/test/java/com/nalbam/webmvc/util/` (`AddressUtilTests`, `PackageUtilTests`, `TimeUtilTests`) use `org.junit.jupiter.api.Test` and `Assertions`.
  `mvn -B clean package` on JDK 25 and Maven 3.10.0 ran 7 tests: 0 failures, 0 errors.

### Jackson 2 and 3

The project needs both major versions. They use different packages and do not conflict.

- Jackson 3, `tools.jackson.core:jackson-databind` 3.2.4: `RestfulController.java` imports `tools.jackson.databind.json.JsonMapper` and `tools.jackson.core.type.TypeReference` and parses the `/loop/{count}` response.
- Jackson 2, `com.fasterxml.jackson.core:jackson-databind` 2.22.4: `src/main/resources/logback.xml` uses `ch.qos.logback.contrib.jackson.JacksonJsonFormatter` from `logback-jackson` 0.1.5.
  That class uses `com.fasterxml.jackson.databind.ObjectMapper`, but `logback-jackson` brings no Jackson in `dependency:tree`. `pom.xml` therefore declares Jackson 2 with the comment `jackson 2 is required by logback-jackson`.
- The WAR contains `jackson-databind-2.22.4.jar`, `jackson-core-2.22.4.jar`, `jackson-databind-3.2.4.jar`, `jackson-core-3.2.4.jar` and `jackson-annotations-2.22.jar` (Jackson 3 uses the 2.x annotations).

Do not remove Jackson 2: JSON logging would fail with a missing class.

### Logback

- `pom.xml` declares `ch.qos.logback:logback-classic` 1.6.6. `dependency:tree` resolves `logback-core` 1.6.6 and `slf4j-api` 2.0.20.

## Applied / Can be closed

Applied in this review: none. This review changes only `docs/open-pr-review.md` and `README.md`. It changes no dependency, code or build file.

Already on `main` before this review:

- `6d9d119` (#181): all current versions and artifacts in the table above.
- `5c287dd` (#185): JDK 25 and Maven 3.10.0 enforcement, `slf4j-api` 2.0.20.

Can be closed, all superseded by `main` at `26e4ee7`:

- [#143](https://github.com/nalbam/sample-tomcat/pull/143) HttpClient 4.5.14 → replaced by `httpclient5` 5.6.5
- [#155](https://github.com/nalbam/sample-tomcat/pull/155) Logback 1.2.13 → 1.6.6
- [#164](https://github.com/nalbam/sample-tomcat/pull/164) `jetty-maven-plugin` 11.0.20 → replaced by `jetty-ee11-maven-plugin` 12.1.14
- [#165](https://github.com/nalbam/sample-tomcat/pull/165) `junit-jupiter-engine` 5.10.2 → `junit-jupiter` 6.1.3
- [#166](https://github.com/nalbam/sample-tomcat/pull/166) `junit-jupiter-api` 5.10.2 → `junit-jupiter` 6.1.3
- [#173](https://github.com/nalbam/sample-tomcat/pull/173) Jackson 2.17.0 → Jackson 2 2.22.4, kept for `logback-jackson`
- [#175](https://github.com/nalbam/sample-tomcat/pull/175) `maven-compiler-plugin` 3.13.0 → 3.16.0
- [#176](https://github.com/nalbam/sample-tomcat/pull/176) Lombok 1.18.32 → 1.18.48
- [#178](https://github.com/nalbam/sample-tomcat/pull/178) `spring-webmvc` 6.1.6 → 7.0.10
- [#180](https://github.com/nalbam/sample-tomcat/pull/180) Logback 1.5.6 → 1.6.6

## Check again

Prerequisites: JDK 25, Maven 3.10.0 or later, and an authenticated `gh`.
`gh api` and `gh pr` need a stored login (`gh auth login`) or a token in `GH_TOKEN` or `GITHUB_TOKEN`. Without one, they print an authentication message and stop.
Check with `gh auth status`.

1. List the open PRs:

   ```bash
   gh api --paginate 'repos/nalbam/sample-tomcat/pulls?state=open&per_page=100' \
     --jq '.[] | [.number, .title, .head.sha] | @tsv'
   ```

   Without `gh` authentication, use the public REST API directly. It accepts anonymous requests at a lower rate limit (60 requests per hour per IP address):

   ```bash
   curl -fsS 'https://api.github.com/repos/nalbam/sample-tomcat/pulls?state=open&per_page=100' \
     | jq -r '.[] | [.number, .title, .head.sha] | @tsv'
   ```

   This returns one page of up to 100 PRs. If there are more, read the next page with `&page=2`.
   For a diff without `gh`, open `https://github.com/nalbam/sample-tomcat/pull/<number>.diff`.

   If the command fails (for example with an authentication, rate limit or network error), the list is unknown. Do not treat it as "no open PRs". Record the time and the error, and run it again.
   A new PR that is not in the table needs its own review. [#187](https://github.com/nalbam/sample-tomcat/pull/187) is the PR that adds this page; it is not a dependency update.

2. Read the change and the checks of one PR:

   ```bash
   gh pr diff 180 -R nalbam/sample-tomcat
   gh pr view 180 -R nalbam/sample-tomcat --json mergeable,statusCheckRollup
   ```

3. Show the resolved dependencies and plugins:

   ```bash
   mvn -B help:effective-pom -Doutput=target/effective-pom.xml
   mvn -B dependency:tree
   ```

   Expected: `httpclient5:5.6.5`, `logback-classic:1.6.6`, `tools.jackson.core:jackson-databind:3.2.4`, `com.fasterxml.jackson.core:jackson-databind:2.22.4`,
   `junit-jupiter-api` and `junit-jupiter-engine` 6.1.3 under `junit-jupiter:6.1.3`, and `org.eclipse.jetty.ee11:jetty-ee11-maven-plugin` 12.1.14 in the effective POM.
   On Maven older than 3.10.0 or JDK older than 25, `maven-enforcer-plugin` stops the build.

If `pom.xml` changes, update the "Current" column and the baseline commit.
