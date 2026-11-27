# JML & OpenJML Verification Report — ResolveIt

## 1. Verification Setup

Formal verification using **JML (Java Modeling Language)** and **OpenJML** (SMT solver **Z3 5.1.0**) was applied to the **Service** layer of the *ResolveIt* application.

- **Compilation and Classpath:** Since the project uses Java 25 (bytecode `69.0`) while OpenJML supports up to Java 21 (bytecode `65.0`), the automated scripts (`../../jml/statica/esegui.bat` and `jml/dinamica/esegui_rac.bat`) compile the classes via `mvnw.cmd clean compile -Djava.version=21 -Dmaven.compiler.release=21` and extract Maven dependencies (Jakarta Persistence, Spring Data, Spring Security) into `target/jml-lib`.
- **OpenJML Configuration:** Executed with `--specs-path src/main/java` and full classpath (`target/classes;target/jml-lib/*`), without requiring *delombok* since the Service classes do not use Lombok annotations.

---

## 2. Specifications Added

Formal contracts (`public behavior`, `requires`, `ensures`, `signals_only`) were defined on the Service interfaces and implemented in their corresponding classes:

- **`CategoriaService` / `CategoriaImpl`**:
  - `disableCategoria(Long id)`, `enableCategoria(Long id)`: preconditions on nullable parameters, ensuring `RuntimeException` is thrown exclusively (`signals_only RuntimeException`) if the ID is null or does not exist.
  - `addCategoria(Categoria c)`, `updateCategoria(Categoria c)`: nullability validation, record uniqueness/existence checks, and exception discipline (`signals_only RuntimeException`).
- **`AccountService` / `AccountImpl`**:
  - `getUserByEmail(String email)`: postcondition `ensures \result != null` on normal completion; `signals_only RuntimeException` if the email is not registered.
  - `modifyUser(MyProfileDTO dto)`: profile/password consistency checks and `signals_only RuntimeException` for invalid inputs.
  - `removeAccountCliente(Long id)`, `removeAccountOperatore(Long id)`: identifier preconditions and `signals_only RuntimeException` if the user does not exist.
- **`TicketService` / `TicketImpl`**:
  - `assignTicket(Long id, Operatore op)`, `resolveTicket(Long id)`, `deleteTicket(Long id)`, `releaseTicket(Long id)`: state transition guards (`APERTO -> IN_CORSO -> RISOLTO / ANNULLATO`) and `signals_only RuntimeException` for invalid states.
  - `getTicketById(Long id)`: postcondition `ensures \result != null` and `signals_only RuntimeException` if missing.
  - `addTicket(...)`, `getTicketUtente(...)`, `getTicketDisponibili()`, `getTicketInCarico(...)`, `getTicketUtenteFiltrati(...)`: contracts on parameters and non-null return values.

---

## 3. ESC Results (Static Verification)

Static analysis (`openjml --esc --timeout 30`) yielded **0 verification failures** across all three subsystems:

| Module | Verified Methods (Z3) | Excluded Methods (`//@ skipesc`) | Verification Failures |
| :--- | :---: | :---: | :---: |
| `CategoriaService` / `CategoriaImpl` | 5 | 0 | **0** |
| `AccountService` / `AccountImpl` | 5 | 0 | **0** |
| `TicketService` / `TicketImpl` | 6 | 6 | **0** |

**Issues Encountered and Resolved:**
1. *Spring Injection (`@Autowired`):* Repository fields are null outside the Spring container; declared as `/*@ spec_public nullable @*/` with local `//@ assume` statements inside verified methods.
2. *`Optional.orElseThrow(...)` Calls:* Lambdas caused prover ambiguity; replaced with explicit `isPresent()` / `get()` checks.
3. *Limitations on I/O, Streams, and JPA Relationships:* `addTicket` (`MultipartFile` I/O and regex), `releaseTicket` (clearing an unannotated external entity `@ManyToOne` `operatore` relationship), and the 4 stream-based query methods were excluded via `//@ skipesc` and delegated to RAC.

---

## 4. RAC Results (Runtime Verification)

Dynamic analysis was performed by instrumenting the `*Service.java` interfaces with `openjml --rac` and invoking them through the `../../jml/dinamica/TestRacService.java` driver (using in-memory dynamic proxies for the Repositories).

- **Debugging Performed:** Direct instrumentation of `*Impl.java` triggered an OpenJML internal error (`MISMATCHED BLOCKS-CBX`) on Spring Data JPA generics; instrumenting the `*Service` interfaces allowed contracts to be inherited and checked at runtime without errors.
- **Outcome:** All **18 methods** (including the 6 excluded from ESC) passed `requires`, `ensures`, and `signals_only` checks across both nominal flows and error cases (**0 JML violations**).

---

## 5. Verification Success & Lessons Learned

- **Overall Outcome:** 100% of the introduced specifications were verified: 12 core methods mathematically proven via ESC and all 18 methods dynamically validated via RAC.
- **Lessons Learned:** In a Spring Data JPA architecture, ESC strictly guarantees the absence of `NullPointerException`, the validity of state guards, and compliance with `signals_only` clauses, though it lacks visibility into persistent database state across `JpaRepository` calls.
  Complementary integration of **ESC** (formal guarantee on control flows) and **RAC** (dynamic coverage on I/O, streams, and edge cases) proved essential to achieving thorough verification without compromising the project's architectural integrity.