# ResolveIt – Test Coverage Analysis & Strategy Report

## 1. JaCoCo Plugin Configuration

The code coverage workflow for the ResolveIt project is configured using **`jacoco-maven-plugin` version `0.8.16`**. The configuration defines specific exclusions, execution phases, and quality gate thresholds to ensure automated compliance during CI builds.

### 1.1 Excluded Components
To focus testing metrics on actionable domain logic, technical infrastructure and simple data structures are excluded from coverage analysis:
* Main application entry point: `it/unisa/resolveIt/ResolveItApplication.class`
* Shared cross-cutting components: `it/unisa/resolveIt/common/**`
* Domain models and JPA entities: `it/unisa/resolveIt/model/**`
* Data Transfer Objects (DTOs): `it/unisa/resolveIt/*/dto/**`

### 1.2 Execution Goals
The plugin executes three key goals across the Maven build lifecycle:
1. **`prepare-agent`** (Phase: default / runtime preparation): Attaches the JaCoCo runtime agent to the JVM, enabling execution tracking during test suites.
2. **`report`** (Phase: `test`): Compiles coverage execution data into human-readable HTML reports and machine-readable XML/CSV artifacts.
3. **`check`** (Phase: `verify`): Evaluates coverage metrics against predefined project quality limits and fails the build if requirements are not met.

### 1.3 Quality Gate Thresholds
The quality gate rules configured in `pom.xml` mandate the following minimum thresholds across all included packages:
* **Line Coverage (`LINE`):** Minimum **$90\%$** covered ratio ($\text{COVEREDRATIO} \ge 0.90$).
* **Branch Coverage (`BRANCH`):** Minimum **$90\%$** covered ratio ($\text{COVEREDRATIO} \ge 0.90$).
* **Class Coverage (`CLASS`):** Minimum **$100\%$** covered ratio ($\text{COVEREDRATIO} = 1.00$).

---

## 2. Coverage Analysis

Based on the execution report across all **$13$ analyzed project classes**, the overall test suite demonstrates high coverage, successfully exceeding all repository quality gate thresholds.

### 2.1 Overall Coverage Summary

| Metric | Covered | Missed | Total | Coverage % | 
| :--- | :---: | :---: | :---: | :---: |
| **Line Coverage** | $377$ | $7$ | $384$ | **$98.18\%$** | 
| **Branch Coverage** | $144$ | $6$ | $150$ | **$96.00\%$** |
| **Class Coverage** | $13$ | $0$ | $13$ | **$100.00\%$** |