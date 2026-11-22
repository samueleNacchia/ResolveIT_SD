# ResolveIt – Mutation Testing Report

Mutation testing measures how effectively the existing tests detect *plausible bugs*. It works by introducing small, realistic faults (mutants) into the code and checking whether the test suite detects them. A high mutation score indicates that the tests are effective at identifying potential defects, while surviving mutants highlight opportunities to improve test quality.

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
  * `mutationThreshold`: **80%** minimum mutation score required to pass.
  * `coverageThreshold`: **80%** minimum line coverage required.

* **Execution & Output Settings:**
  * `timeoutConstant`: `5000 ms`, with a `timeoutFactor` of `1.5` to handle potentially slow or infinite executions caused by mutations.
  * `jvmArgs`: Maximum heap memory of `1024 MB`, dynamic agent loading enabled (`-XX:+EnableDynamicAgentLoading`), and modular reflection access (`--add-opens=java.base/java.lang=ALL-UNNAMED`).
  * `outputFormats`: Both `HTML` (for human review) and `XML` (for automated reporting tools).
  * JUnit 5 support is provided by `pitest-junit5-plugin`, version `1.2.3`.
  * Verbose execution is enabled.

> **Status in CI:** The mutation-testing steps are currently commented out in `.github/workflows/ci.yml`, so PIT runs only on demand. To execute it locally:
> ```bash
> mvn org.pitest:pitest-maven:mutationCoverage
> ```

## 2. Mutation Analysis

### Overall Results Summary

The latest mutation-testing execution evaluated **218 mutations**, of which **179 were killed**, achieving an overall mutation score of **82%** according to PIT's report.

This exceeds the configured minimum mutation threshold of 80%, meaning the project successfully satisfies its mutation-testing quality gate.

| Metric | Value |
| :--- | :---: |
| Total Mutations Generated | 218 |
| Killed Mutations | 179 |
| Survived Mutations | 39 |
| Mutations with No Coverage | 4 |
| Overall Mutation Score | **82%** |
| Test Strength | **84%** |
| Line Coverage (mutated classes only) | **98% (375/384)** |
| Tests Examined | 40 |
| Tests Executed | 472 |
| Tests per Mutation | 2.17 |
| Total Execution Time | 115 seconds |

A mutation score above the configured threshold demonstrates that the test suite detects most of the injected faults. However, the surviving mutations indicate that some behaviors could benefit from more precise assertions and additional edge-case testing.

### Mutation Results by Mutator

The following table presents the results for each mutation operator used by PIT.

| Mutator | Generated | Killed | Survived | Kill Rate |
| :--- | ---: | ---: | ---: | ---: |
| Conditionals Boundary | 3 | 2 | 1 | 67% |
| Primitive Returns | 3 | 2 | 1 | 67% |
| Void Method Call | 76 | 44 | 30 | 58% |
| Boolean True Returns | 4 | 4 | 0 | 100% |
| Null Returns | 11 | 10 | 0 | 91% |
| Empty Object Returns | 43 | 41 | 1 | 95% |
| Boolean False Returns | 3 | 3 | 0 | 100% |
| Negate Conditionals | 75 | 73 | 2 | 97% |
| **Total** | **218** | **179** | **35** | **82%** |

*Note: The per-mutator survivor counts above exclude mutants with no coverage. The overall total of 39 survivors includes these 4 mutants.*

### Areas Requiring Improvement

The results identify a few areas where additional testing could improve the mutation score:

* **Void Method Call Mutator (58%):** This operator accounts for the largest number of surviving mutations: 30 out of 76 generated mutations. These mutants indicate that some method invocations can be removed without causing existing tests to fail. Tests should verify relevant side effects, interactions with dependencies, and changes to application state where appropriate.

* **Conditionals Boundary Mutator (67%):** One mutation survived out of three. Additional tests should exercise boundary conditions, particularly values immediately below, at, and above relevant limits.

* **Primitive Returns Mutator (67%):** One mutation survived out of three. Tests should verify the expected primitive return values for different execution paths.

* **Negate Conditionals Mutator (97%):** Two mutations survived out of 75. Although the result is very good, reviewing the affected conditional branches and adding tests for less common execution paths could improve it further.

* **Empty Object Returns Mutator (95%):** One mutation survived out of 43. The corresponding code should be reviewed to determine whether the alternative return value represents an observable behavioral difference.

Additionally, **four mutations had no coverage**, indicating that the corresponding code was not exercised by the test suite during mutation analysis. These areas should be investigated to determine whether additional tests are necessary.

### Execution Performance

PIT completed the mutation analysis in **115 seconds (1 minute and 55 seconds)**.

| Execution Metric | Result |
| --- | :---: |
| Pre-scan for mutations | Less than 1 second |
| Classpath scanning | Less than 1 second |
| Coverage and dependency analysis | 16 seconds |
| Mutation test generation | Less than 1 second |
| Mutation analysis execution | 98 seconds |
| Total execution time | 115 seconds |
| Tests exceeding 2000 ms | 0 |
| Slowest test | 309 ms |
| Largest test coverage | 269 blocks |

The execution results indicate that the mutation-testing process completes within a reasonable time. No tests exceeded the 2000 ms reporting threshold.

## 3. Conclusion

The latest PIT execution demonstrates that ResolveIt has a solid level of test effectiveness:

- **82% mutation score**, exceeding the configured 80% threshold.
- **98% line coverage** across mutated classes.
- **84% test strength**, indicating that the test suite successfully detects most generated mutations.
- **472 test executions** completed in approximately 115 seconds.

The most significant opportunity for improvement lies in the `VoidMethodCallMutator`, which has the lowest kill rate (58%) and the largest number of surviving mutations.

Future testing improvements should prioritize verifying method side effects, strengthening boundary-condition assertions, and covering the four mutations that currently have no test coverage.

Overall, the project satisfies its configured mutation-testing quality gate and demonstrates good test effectiveness, while leaving identifiable opportunities for further improvement.