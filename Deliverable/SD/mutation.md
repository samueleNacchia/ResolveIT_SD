# ResolveIt – Mutation Testing Report

Mutation testing measures how effectively the existing tests would catch *plausible bugs*. It works by introducing small, realistic faults into the code (mutants) and checking whether the test suite "kills" them. A high mutation score means the tests are strong; a low score points to weakly tested areas.

## 1. Mutation Testing Configuration Parameters

The configuration lives in `pom.xml` (`org.pitest:pitest-maven`, version `1.30.2`).

* **Target Scope:**
  * `targetClasses`: `it.unisa.resolveIt.*`
  * `targetTests`: `it.unisa.resolveIt.*`
* **Exclusions (`excludedClasses`):** Data structures and infrastructure classes are excluded to focus testing on business logic:
  * Application entry point: `it.unisa.resolveIt.ResolveItApplication`
  * Shared utilities: `it.unisa.resolveIt.common.*`
  * Domain entities: `it.unisa.resolveIt.model.*`
  * DTOs: `it.unisa.resolveIt.*.dto.*`
* **Quality Gate Thresholds:**
  * `mutationThreshold`: $80\%$ minimum mutation score required to pass.
  * `coverageThreshold`: $80\%$ minimum line coverage required (so mutants are only evaluated on top of a solid coverage base).
* **Execution & Output Settings:**
  * `timeoutConstant`: $5000\text{ ms}$ with a `timeoutFactor` of $1.5$ to handle potential infinite loops caused by mutations.
  * `jvmArgs`: Dynamic agent loading enabled (`-XX:+EnableDynamicAgentLoading`) with modular reflection access (`--add-opens=java.base/java.lang=ALL-UNNAMED`).
  * `outputFormats`: Both `HTML` (for human review) and `XML` (for automated reporting tools).

> **Status in CI:** this configuration is complete, but the mutation-testing steps are currently **commented out** in `.github/workflows/ci.yml`, so PiTest runs only on demand. To execute it locally:
> ```bash
> mvn org.pitest:pitest-maven:mutationCoverage
> ```

## 2. Mutation Analysis

### Overall Results Summary

Across all evaluated classes in the application domain, the test suite achieved a **Mutation Score of $83.53\%$**, successfully exceeding the mandatory $80\%$ quality gate threshold.

| Metric | Value |
| :--- | :---: |
| Total Mutations Generated | $164\%$ |
| Killed Mutations | $137\%$ |
| Survived Mutations | $27\%$ |
| Overall Mutation Score | $83.53\%$  |

A score above the gate confirms that, overall, the tests detect most injected faults. The surviving mutants are concentrated in specific areas (below), which represent the best opportunities to strengthen test quality.

### Areas with Low Mutation Coverage

* **`GestoreController` (25.00\%):** Represents the lowest mutation score due to missing unit tests for administrative endpoints, resulting in unkilled mutators for default return values and branch conditions. Adding focused tests for these endpoints would raise both coverage and the mutation score.
* **`RegistrazioneController` (71.43\%):** Survived mutations are concentrated around boundary checks on input fields during registration validation — e.g. edge cases at the limits of length or format constraints. Tightening those assertions (including boundary values) would kill more mutants.
