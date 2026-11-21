# ResolveIt – CI/CD Workflow

This document describes the two GitHub Actions workflows that automate the quality and delivery pipeline of the ResolveIt project:

- **CI** (`.github/workflows/ci.yml`) — builds, tests, measures coverage and mutation score, and uploads the results.
- **CD** (`.github/workflows/docker-build.yml`) — pushes a multi-platform Docker image to Docker Hub when the CI succeeds on a pull request.

Together they form a single pipeline: every change that lands in `main`, or is about to land via a pull request, is verified automatically and then packaged as a container image.

## When it runs

The **CI** workflow starts in three cases:

- **push** to the `main` branch;
- **pull request** targeting `main`;
- **workflow_dispatch** — manual triggering from the Actions tab.

The **CD** workflow is *not* triggered directly by pushes or pull requests. It reacts to the completion of a CI run (`workflow_run`) and only builds an image when:

- the referenced CI run finished with conclusion `success`, **and**
- that CI run was caused by a `pull_request` event (so images are never pushed for direct commits to `main`).

This way, every change that lands in `main`, or is about to, is verified automatically.

## Permissions

```yaml
permissions:
  contents: read
  actions: read      # only required by the CD workflow
```

Both workflows have **read-only** access to the repository (`contents: read`) and can read their own metadata and cached artifacts (`actions: read`). They cannot create commits or push code, which limits the damage if something goes wrong. The only write happens *outside* the repo: the CI uploads a JAR artifact externally, and the CD pushes an image to Docker Hub using secrets.

## Pipeline overview

```
┌─────────────┐   push / PR    ┌──────────────────────────┐   success on PR   ┌──────────────────────────┐
│  Developer   │ ───────────►  │        CI job             │ ────────────────► │     CD job            │
│  commits     │               │ checkout → JDK25 → cache  │                   │ login → buildx → push    │
└─────────────┘               │ mvn verify (H2)           │                   │ multi-platform image      │
                              │ JaCoCo + PiTest reports    │                   │ to Docker Hub             │
                              └──────────────────────────┘                   └──────────────────────────┘
```

## The CI job (`ci.yml`)

There is a single job, `ci`, running on an `ubuntu-26.04` virtual machine. Despite its name, the job does not just prepare the environment: it also runs the tests and uploads every quality report at the end.

> **Note:** no external database is required on the runner. The integration tests run against an **embedded H2 database** (test scope) with an embedded Tomcat server provided by Spring Boot, so the job needs no MySQL service.

### 1. Checkout the code

`actions/checkout@v7` downloads the repository code into the virtual machine so the following steps can work on it.

### 2. Set up JDK 25

`actions/setup-java@v6` configures Java 25 with the Temurin distribution (`java-version: '25'`, `distribution: 'temurin'`). Because this is exactly the default JDK of the `ubuntu-26.04` image, **no download happens** — a download only occurs if the image's default JDK changes or the distribution becomes outdated.

### 3. Cache Maven dependencies

`actions/cache@v6` saves and restores the `~/.m2/repository` folder, where Maven keeps downloaded dependencies. This avoids downloading them again on every run.

- **`key`**: combines the operating system and the hash of all `pom.xml` files (`${{ runner.os }}-resolveit-${{ hashFiles('**/pom.xml') }}`). If the dependency tree has not changed, the exact cache is reused.
- **`restore-keys`**: if no cache matches the exact key, it falls back to an earlier one with the same prefix (`${{ runner.os }}-resolveit-`), which is then updated. Only the new dependencies are downloaded.

### 4. Integration tests

```bash
mvn verify --file pom.xml
```

Maven recompiles the project and runs everything up to the `verify` phase, including the integration tests (which use the embedded H2 database). There is no need for `mvn clean`, because the runner is ephemeral: the VM is reset between workflow runs, so the `target` folder always starts clean.

If a test fails, the workflow is marked as failed, so the problem is immediately visible in the pull request.

### 5. Generate JaCoCo report

```bash
mvn jacoco:report
```

Regenerates the JaCoCo XML/HTML coverage data (see [coverage.md](./coverage.md)) before it is uploaded to Codecov.

### 6. Display test results

`dorny/test-reporter@v2` parses the JUnit XML files in `target/surefire-reports/*.xml` and renders them as a browsable report attached to the run.

### 7. Upload coverage to Codecov

```yaml
uses: codecov/codecov-action@v4
with:
  token: ${{ secrets.CODECOV_TOKEN }}
  file: ./target/site/jacoco/jacoco.xml
  flags: unittests
  name: codecov-umbrella
  fail_ci_if_error: false
```

Uploads the JaCoCo XML to Codecov (authenticated with `CODECOV_TOKEN`). `fail_ci_if_error: false` keeps the build green even if Codecov is unreachable.

### 8. Upload test results to Codecov

```yaml
uses: codecov/test-results-action@v1
with:
  token: ${{ secrets.CODECOV_TOKEN }}
  file: ./target/surefire-reports/TEST-*.xml
```

Uploads the raw JUnit results so Codecov can compute additional metrics. Skipped only when the run is cancelled (`if: !cancelled()`).

### 9. Upload JAR artifact (for CD)

```yaml
uses: actions/upload-artifact@v7
with:
  name: resolveit-${{ github.sha }}
  path: |
    target/resolveit-0.0.1-SNAPSHOT.jar
    target/surefire-reports/
  retention-days: 7
```

The compiled Spring Boot JAR (and the test reports) are saved **externally** from the ephemeral VM, ready for the CD job to download and package. Artifacts are retained for 7 days.

### Mutation testing — configured but disabled in CI

PiTest mutation testing is fully configured in `pom.xml` ([mutation.md](./mutation.md)), with an `80%` mutation-score gate. It is **not** executed by the current CI workflow: the corresponding steps in `ci.yml` are commented out and run only on demand (e.g. manually or during a dedicated job). To enable it, uncomment those steps.

## The CD job (`docker-build.yml`)

The CD job builds and pushes the application image to Docker Hub. Its steps:

1. **Checkout** the source at the exact commit that produced the successful CI run (`ref: head_sha`).
2. **Download the JAR artifact** uploaded by CI, named `resolveit-<head-sha>`, into `target/`.
3. **Set up Docker Buildx** for reliable, multi-platform builds.
4. **Log in to Docker Hub** using `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets.
5. **Extract metadata** (`docker/metadata-action@v6`) to derive the image name `${DOCKER_USERNAME}/resolveit_sd` and two tags: `latest` and `sha-<short-hash>`.
6. **Build and push** with `docker/build-push-action@v7`, targeting both `linux/amd64` and `linux/arm64`, pushing to the remote registry, and using GitHub Actions cache (`cache-from`/`cache-to: type=gha`) to speed up subsequent builds.

The resulting image is documented in [Docker.md](./Docker.md).
