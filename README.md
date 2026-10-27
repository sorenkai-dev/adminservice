# Administration Microservice

[![CI](https://github.com/sorenkai-dev/adminservice/actions/workflows/ci.yml/badge.svg)](https://github.com/sorenkai-dev/adminservice/actions/workflows/ci.yml)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![JDK](https://img.shields.io/badge/JDK-27-brightgreen.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![gRPC](https://img.shields.io/badge/gRPC-1.85.0-244c5a.svg)](https://grpc.io/)
[![Code Style: Spotless](https://img.shields.io/badge/code%20style-Spotless%20%2F%20ktlint-informational.svg)](https://github.com/diffplug/spotless)
[![Static Analysis: Detekt](https://img.shields.io/badge/static%20analysis-Detekt-blueviolet.svg)](https://detekt.dev/)
[![Docker](https://img.shields.io/badge/Docker-enabled-2496ED.svg?logo=docker&logoColor=white)](Dockerfile)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)


A multi-module Kotlin JVM gRPC microservice targeting JVM 26 (JDK 27), built with gRPC-Kotlin, Protocol Buffers, and Netty.

## Project Structure

This project follows a clean multi-module architecture for a standalone Kotlin JVM application:

- `app` — Application entry point, gRPC server host, and health check services
- `api` — Protobuf definitions and generated gRPC Kotlin/Java stubs
- `core` — Business logic and domain models (independent of transport and persistence implementations)
- `persistence` — Database access and Flyway migrations

The build is configured using Gradle Kotlin DSL scripts, version catalogs (`gradle/libs.versions.toml`), and Foojay toolchain resolver convention targeting JDK 27.

---

## Build & Test

This project uses the [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html). Ensure you have JDK 27 installed or let Gradle Toolchains manage it automatically.

### Common Gradle Commands

* **Build entire project:**
  ```bash
  ./gradlew build
  ```
* **Run application locally:**
  ```bash
  ./gradlew :app:run
  ```
* **Run tests:**
  ```bash
  ./gradlew test
  ```
* **Run all verification checks (tests, Spotless, Detekt):**
  ```bash
  ./gradlew check
  ```
* **Run Spotless code style checks:**
  ```bash
  ./gradlew spotlessCheck
  ```
* **Apply Spotless code formatting:**
  ```bash
  ./gradlew spotlessApply
  ```
* **Run Detekt static analysis:**
  ```bash
  ./gradlew detekt
  ```
* **Install Git pre-commit hooks (Spotless & Detekt):**
  ```bash
  ./gradlew installGitHooks
  ```
* **Clean build outputs:**
  ```bash
  ./gradlew clean
  ```

---

## Docker

A multi-stage `Dockerfile` is provided to build and package the `app` module into a secure, minimal container image.

### Features
- **Multi-Stage Build:** Uses `eclipse-temurin:27-jdk` for compilation/packaging and `eclipse-temurin:27-jre` for a lightweight runtime footprint.
- **Non-Root Execution:** Runs under a dedicated `appuser` (UID/GID `10001`).
- **Cloud Run Compatible:** Respects the `PORT` environment variable (defaults to `8080`).
- **Signal Handling & Graceful Shutdown:** Preserves `SIGTERM` propagation to the JVM runtime.
- **Embedded Health Probe:** Includes `grpc_health_probe` for gRPC container health checks.

### Building & Running with Docker

1. **Build Docker image:**
   ```bash
   docker build -t adminservice:latest .
   ```

2. **Run container:**
   ```bash
   docker run -p 8080:8080 -e PORT=8080 adminservice:latest
   ```

3. **Verify gRPC Health Check:**
   ```bash
   docker exec -it <container_id> /usr/local/bin/grpc_health_probe -addr=localhost:8080 -service=sorenkai.admin.v1.AdminService
   ```

---

## Docker Compose (Local Development)

Docker Compose configures the Admin microservice and a PostgreSQL database for local development.

### Services
- **`admin`**: The gRPC Admin microservice listening on port `8080`, with gRPC-aware health checks using `grpc_health_probe`.
- **`postgres`**: PostgreSQL 17 database with persistent data volume (`postgres_data`) and `pg_isready` health checking.

### Usage

1. **(Optional) Configure environment:**
   Copy `.env.example` to `.env` to customize default ports and credentials:
   ```bash
   cp .env.example .env
   ```

2. **Start all services in detached mode:**
   ```bash
   docker compose up -d
   ```

3. **Check service status and health:**
   ```bash
   docker compose ps
   ```

4. **View logs:**
   ```bash
   docker compose logs -f admin
   ```

5. **Stop services:**
   ```bash
   docker compose down
   ```
   *(To remove persistent database volumes as well, run `docker compose down -v`)*

---

## Continuous Integration (GitHub Actions)

A GitHub Actions CI workflow (`.github/workflows/ci.yml`) runs automatically on pushes and pull requests to `main`/`master` branches:

- **Gradle Build & Verification:**
  - Configures JDK 27 (`temurin`) with dependency caching.
  - Runs Spotless style checks (`./gradlew spotlessCheck`).
  - Runs Detekt static code analysis (`./gradlew detekt`).
  - Runs JUnit test suite and builds all artifacts (`./gradlew build`).
- **Docker Validation:**
  - Validates container packaging by building the multi-stage Docker image using Docker Buildx.

---

## Contributing

Contributions are welcome! Please refer to the [Contributing Guidelines](CONTRIBUTING.md) for details on code style, testing standards, Git pre-commit hooks, and the pull request review process.

---

## Security

For security policies, scope, and instructions on reporting vulnerabilities responsibly, please review our [Security Policy](SECURITY.md).

---

## License

This project is licensed under the Apache License, Version 2.0. See the [LICENSE](LICENSE) and [NOTICE](NOTICE) files for details.