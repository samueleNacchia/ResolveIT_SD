# ResolveIt

A Spring Boot application that resolves and manages requests/tickets, built with Java 25 and packaged as a container image.

## Tech stack

- **Framework:** Spring Boot `4.1.2` (Web MVC + Thymeleaf, Spring Data JPA, Spring Security, Actuator)
- **Language / JDK:** Java 25 (Temurin)
- **Databases:** MySQL `9.7` in production (`docker-compose-deploy.yml`) · embedded H2 for CI tests
- **Build tool:** Maven (`pom.xml`) with JaCoCo coverage and PiTest mutation testing configured
- **Containerization:** Dockerfile + Docker Compose, multi-platform image pushed to Docker Hub

## Quality & delivery pipeline

Automated GitHub Actions workflows keep the code tested and packaged:

| Workflow | File | Purpose |
| :--- | :--- | :--- |
| CI | `.github/workflows/ci.yml` | Checkout → JDK 25 → Maven cache → `mvn verify` (tests) → JaCoCo → upload to Codecov → save JAR artifact |
| CD | `.github/workflows/docker-build.yml` | On a successful PR, build & push a multi-platform image (`linux/amd64`, `linux/arm64`) to Docker Hub |

## Documentation

- [Continuous Integration & Delivery (CI/CD)](/Deliverable/SD/CICD.md)
- [Containerization & Docker](/Deliverable/SD/Docker.md)
- [Test Coverage Analysis](/Deliverable/SD/coverage.md)
- [Mutation Testing Report](/Deliverable/SD/mutation.md)

## Deploy locally with Docker Compose

```bash
# 1. create a .env file (see .env.example):
#    MYSQL_DATABASE=resolveit   MYSQL_ROOT_PASSWORD=root
#    MYSQL_USER=appuser         MYSQL_PASSWORD=secret
#    DB_PORT=3306               APP_PORT=8080

docker compose -f docker-compose.deploy.yml up -d          # downloads the image, then start db + web in the background
docker compose logs -f web    # follow application logs
```

The `web` service starts only after the MySQL `db` service reports healthy.
