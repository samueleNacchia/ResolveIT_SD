# ResolveIt – CI Workflow

This document describes the ResolveIt GitHub Actions workflow, which builds the project and runs the integration tests every time the code changes.

## When it runs

The workflow starts in two cases:

- a **push** to the `main` branch;
- a **pull request** targeting `main`.

This way, every change that lands in `main`, or is about to, is verified automatically.

## Permissions

```yaml
permissions:
  contents: read
```

The workflow has read-only access to the repository. It cannot write to the repository, which limits the damage if something goes wrong.

## The job

There is a single job, `ci`, running on an `ubuntu-26.04` virtual machine. Despite its name, the job does not just prepare the environment: it also runs the tests at the end.

### 1. Start the MySQL 8.4 database

MySQL 8.4 is already installed in the Ubuntu image, but the service is stopped by default. The command `sudo systemctl start mysql.service` starts it with the default configuration: user `root`, password `root`, port `3306`.

### 2. Checkout the code

`actions/checkout` downloads the repository code into the virtual machine so the following steps can work on it.

### 3. Set up JDK 25

`actions/setup-java` configures Java 25 with the Temurin distribution. If the requested version is already present on the machine, it is used and nothing is downloaded. A download only happens if the image's default JDK changes or the distribution becomes outdated.

### 4. Cache Maven dependencies

`actions/cache` saves and restores the `~/.m2/repository` folder, where Maven keeps downloaded dependencies. This avoids downloading them again on every run.

- **`key`**: combines the operating system and the hash of all `pom.xml` files. If the `pom.xml` files have not changed, the exact cache is reused.
- **`restore-keys`**: if no cache matches the exact key, it falls back to an earlier one with the same prefix, which is then updated. Only the new dependencies are downloaded.

### 5. Integration tests

```bash
mvn verify --file pom.xml
```

Maven recompiles the project and runs everything up to the `verify` phase, including the integration tests, which use the MySQL database started in the first step. There is no need for `mvn clean`, because the `target` folder does not persist between runs and always starts clean.

If a test fails, the workflow is marked as failed, so the problem is immediately visible in the pull request.