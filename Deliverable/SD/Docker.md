# Containerization Report — ResolveIt

## 1. Application Image (`Dockerfile`)

The Spring Boot application is packaged into a standalone container image built on top of `eclipse-temurin:25-jre-resolute`, using a JRE-only image with an explicit version tag rather than `:latest` to keep the runtime lightweight and builds deterministic.

Key design and security choices in the `Dockerfile`:
* **Explicit Artifact Copy:** Uses `COPY` instead of `ADD` to copy the compiled JAR from `target/` into `/app/app.jar` without unintended archive extraction.
* **Least-Privilege Execution:** Creates a dedicated unprivileged user (`appuser`), assigns ownership of `/app`, and switches to `USER appuser` so the JVM does not run as `root`.
* **Exec-Form Entrypoint:** Uses `CMD ["java", "-jar", "app.jar"]` so the Java process runs as PID 1 and handles termination signals (`SIGTERM`) cleanly.

---

## 2. Multi-Container Orchestration (`docker-compose.yml`)

To decouple the application logic from persistent storage, the stack is orchestrated via Docker Compose with two services: `web` (the Spring Boot application) and `db` (pinned to `mysql:8.4`).

Key orchestration and resilience choices:
* **Secrets Management:** Database credentials and configuration parameters are externalized via environment variables loaded from a local `.env` file (ignored by Git via `.gitignore`), avoiding hardcoded secrets in source control.
* **Data Durability:** A named volume (`mysql_data`) is mounted to `/var/lib/mysql` so database state persists across container restarts and stack recreations (`docker compose down` / `up`).
* **Health Checks & Startup Ordering:** The `db` service defines a `mysqladmin ping` health check, and `web` uses `depends_on` with `condition: service_healthy` to ensure the backend starts only once MySQL is ready to accept connections.
* **Automatic Recovery:** Both services use `restart: on-failure` to recover automatically from unexpected process crashes.