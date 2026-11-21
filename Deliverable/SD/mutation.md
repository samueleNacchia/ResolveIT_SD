# ResolveIt – Mutation Testing Report

## 1. Mutation Testing Configuration Parameters

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
  * `coverageThreshold`: $80\%$ minimum line coverage required.
* **Execution & Output Settings:**
  * `timeoutConstant`: $5000\text{ ms}$ with a `timeoutFactor` of $1.5$ to handle potential infinite loops caused by mutations.
  * `jvmArgs`: Dynamic agent loading enabled (`-XX:+EnableDynamicAgentLoading`) with modular reflection access (`--add-opens=java.base/java.lang=ALL-UNNAMED`).
  * `outputFormats`: Both `HTML` (for human review) and `XML` (for automated reporting tools).

## 2. Mutation Analysis

### Overall Results Summary

Across all evaluated classes in the application domain, the test suite achieved a **Mutation Score of $83.53\%$**, successfully exceeding the mandatory $80\%$ quality gate threshold.

* **Total Mutations Generated:** $164$
* **Killed Mutations:** $137$
* **Survived Mutations:** $27$
* **Overall Mutation Score:** $83.53\%$ ($\frac{137}{164} \times 100$)

### Areas with Low Mutation Coverage

* **`GestoreController` ($25.00\%$):** Represents the lowest mutation score due to missing unit tests for administrative endpoints, resulting in unkilled mutators for default return values and branch conditions.
* **`RegistrazioneController` ($71.43\%$):** Survived mutations are concentrated around boundary checks on input fields during registration validation.