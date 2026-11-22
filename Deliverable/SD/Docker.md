# Containerization Report — ResolveIt

## 1. Application Image (`Dockerfile`)

The Spring Boot application is packaged into a standalone container image built on top of `eclipse-temurin:25-jre-resolute`, using a **JRE-only** image (no JDK) with an explicit version tag rather than `:latest`, to keep the runtime lightweight and make builds deterministic.

Key design and security choices in the `Dockerfile`:
* **Explicit Artifact Copy:** Uses `COPY` instead of `ADD` to copy the compiled JAR from `target/` into `/app/app.jar`, avoiding any unintended archive extraction (a behaviour `ADD` would trigger for URLs or local tarballs).
* **Least-Privilege Execution:** Creates a dedicated unprivileged user (`appuser`), assigns ownership of `/app` with `chown -R appuser:appuser /app`, and switches to `USER appuser` so the JVM does not run as `root`.
* **Exec-Form Entrypoint:** Uses `CMD ["java", "-jar", "app.jar"]` (exec form, not shell form) so the Java process runs as PID 1 and handles termination signals (`SIGTERM`) cleanly for graceful shutdown.
* **Build Argument:** The listening port is injected at build time via `ARG PORT`, exposed with `EXPOSE ${PORT}`. In `docker-compose.yml` it is set to `8080`.

```dockerfile
FROM eclipse-temurin:25-jre-resolute
ARG PORT
WORKDIR /app
COPY target/*.jar app.jar
RUN useradd -m appuser && chown -R appuser:appuser /app
USER appuser
EXPOSE ${PORT}
CMD ["java", "-jar", "app.jar"]
```

> **Tip:** build with `docker build --build-arg PORT=8080 -t resolveit:latest .` when building outside of Compose.

---

## 2. Multi-Container Orchestration (`docker-compose.yml`)

To decouple the application logic from persistent storage, the stack is orchestrated via Docker Compose with two services: `web` (the Spring Boot application) and `db` (pinned to `mysql:9.7`).

Key orchestration and resilience choices:
* **Secrets Management:** Database credentials and configuration parameters are externalized via environment variables loaded from a local `.env` file (ignored by Git via `.gitignore`), avoiding hardcoded secrets in source control. The compose project reads `${MYSQL_DATABASE}`, `${MYSQL_ROOT_PASSWORD}`, `${MYSQL_USER}`, `${MYSQL_PASSWORD}`, `${DB_PORT}` and `${APP_PORT}`.
* **Data Durability:** A named volume (`mysql_data`) is mounted to `/var/lib/mysql` so database state persists across container restarts and stack recreations (`docker compose down` / `up`).
* **Health Checks & Startup Ordering:** The `db` service defines a `mysqladmin ping` health check, and `web` uses `depends_on` with `condition: service_healthy` to ensure the backend starts only once MySQL is ready to accept connections.
* **Automatic Recovery:** Both services use `restart: on-failure` to recover automatically from unexpected process crashes.
* **Networking:** A single user-defined bridge network (`resolveit_bridge`) connects both services, so `web` reaches the database via its service name `db`.

```yaml
services:
  db:
    image: mysql:9.7
    restart: on-failure
    environment:
      MYSQL_DATABASE: ${MYSQL_DATABASE}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
    ports:
      - ${DB_PORT}:3306
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s
  web:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        PORT: 8080
    restart: on-failure
    ports:
      - ${APP_PORT}:8080
    environment:
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: com.mysql.cj.jdbc.Driver
      SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/${MYSQL_DATABASE}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
      SPRING_DATASOURCE_USERNAME: ${MYSQL_USER}
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_PASSWORD}
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
    depends_on:
      db:
        condition: service_healthy
```

### Local development with the stack

```bash
# create a .env file, e.g.:
#   MYSQL_DATABASE=resolveit
#   MYSQL_ROOT_PASSWORD=root
#   MYSQL_USER=appuser
#   MYSQL_PASSWORD=secret
#   DB_PORT=3306
#   APP_PORT=8080

docker compose up -d        # start db + web in the background
docker compose logs -f web  # follow application logs
```

---

## 3. Container Build & Push Pipeline (`docker-build.yml`)

The image is not built manually — it is produced by the **CD workflow** (see [CICD.md](./CICD.md)). Triggered when a CI run completes successfully after a pull request, the CD job:

1. Checks out the exact commit that passed CI (`head_sha`).
2. Downloads the `resolveit-<sha>` JAR artifact produced by CI into `target/`.
3. Sets up Docker Buildx and logs in to Docker Hub with secrets.
4. Builds a **multi-platform** image (`linux/amd64`, `linux/arm64`) of `${DOCKER_USERNAME}/resolveit_sd` and pushes it, tagged both `latest` and `sha-<short-hash>`.

This guarantees that the image pushed to the registry always matches the exact code that passed the tests.
