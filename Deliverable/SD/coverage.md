# ResolveIt – Test Coverage Analysis & Strategy Report

## 1. JaCoCo Plugin Configuration

The code coverage workflow for the ResolveIt project is configured using **`jacoco-maven-plugin` version `0.8.16`**. The configuration defines specific exclusions, execution phases, and quality gate thresholds to ensure automated compliance during CI builds.

### 1.1 Excluded Components
To focus testing metrics on actionable domain logic, technical infrastructure and simple data structures are excluded from coverage analysis:
* Main application entry point: `it/unisa/resolveIt/ResolveItApplication.class`
* Shared cross-cutting components: `it/unisa/resolveIt/common/**`
* Domain models and JPA entities: `it/unisa/resolveIt/model/**`
* Data Transfer Objects (DTOs): `it.unisa.resolveIt.*.dto.*`

These classes are either boilerplate, infrastructure glue, or pure data holders; testing them would inflate the coverage numbers without adding meaningful assurance about business behaviour.

### 1.2 Execution Goals
The plugin executes three key goals across the Maven build lifecycle:
1. **`prepare-agent`** (Phase: default / runtime preparation): Attaches the JaCoCo runtime agent to the JVM, enabling execution tracking during test suites.
2. **`report`** (Phase: `test`): Compiles coverage execution data into human-readable HTML reports and machine-readable XML/CSV artifacts (`target/site/jacoco/jacoco.xml`).
3. **`check`** (Phase: `verify`): Evaluates coverage metrics against the predefined project quality limits and fails the build if any limit is not met.

### 1.3 Quality Gate Thresholds
The quality gate rules configured in `pom.xml` mandate the following minimum thresholds across all included packages:
* **Line Coverage (`LINE`):** Minimum **90\%** covered ratio .
* **Branch Coverage (`BRANCH`):** Minimum **90\%** covered ratio .
* **Class Coverage (`CLASS`):** Minimum **100\%** covered ratio — every included class must be touched by at least one test.

---

## 2. How it runs in CI

Coverage is produced and reported automatically during the CI workflow (`.github/workflows/ci.yml`):

```bash
mvn verify --file pom.xml   # runs tests, then jacoco:check (quality gate)
mvn jacoco:report            # regenerates target/site/jacoco/jacoco.xml
```

The JaCoCo XML is uploaded to **Codecov** (`codecov/codecov-action@v4`, flag `unittests`), and the raw JUnit results are uploaded separately so Codecov can compute additional metrics. See [CICD.md](./CICD.md) for the full pipeline.

---

## 3. Coverage Analysis

Based on the execution report across all **13 analyzed project classes**, the overall test suite demonstrates high coverage, successfully exceeding all repository quality gate thresholds.

### 3.1 Overall Coverage Summary

| Metric | Covered | Missed | Total | Coverage %  | 
| :--- | :---: | :---: | :---: | :---: |
| **Line Coverage** | $377$ | $7$ | $384$ | **98.18\%** | 
| **Branch Coverage** | $144$ | $6$ | $150$ | **96.00\%** |
| **Class Coverage** | $13$ | $0$ | $13$ | **100.00\%** |

All three metrics exceed the configured gates ( $90\%$ line, $90\%$ branch, $100\%$ class), so the `jacoco:check` goal passes and the build remains green.
