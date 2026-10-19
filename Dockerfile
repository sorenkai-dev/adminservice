# Multi-stage Dockerfile for Admin Service

# Stage 1: Build stage using JDK 27
FROM eclipse-temurin:27-jdk AS builder

ARG GRPC_HEALTH_PROBE_VERSION=v0.4.37

WORKDIR /workspace

# Install curl and ca-certificates to download grpc_health_probe for the target architecture
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    ARCH=$(dpkg --print-architecture) && \
    curl -Lf "https://github.com/grpc-ecosystem/grpc-health-probe/releases/download/${GRPC_HEALTH_PROBE_VERSION}/grpc_health_probe-linux-${ARCH}" -o /usr/local/bin/grpc_health_probe && \
    chmod +x /usr/local/bin/grpc_health_probe && \
    rm -rf /var/lib/apt/lists/*

# Copy Gradle wrapper and configuration files first to leverage build cache
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties detekt.yml ./
COPY gradle gradle
COPY api/build.gradle.kts api/
COPY core/build.gradle.kts core/
COPY persistence/build.gradle.kts persistence/
COPY app/build.gradle.kts app/

# Copy module source code
COPY api api
COPY core core
COPY persistence persistence
COPY app app

# Ensure the Gradle wrapper uses Unix line endings and is executable
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Build the application distribution for the app module
RUN ./gradlew :app:installDist --no-daemon

# Stage 2: Minimal runtime stage using Java 27 compatible runtime
FROM eclipse-temurin:27-jre AS runtime

# Create dedicated non-root user and group
RUN groupadd --system --gid 10001 appuser && \
    useradd --system --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin appuser

WORKDIR /app

# Copy built application distribution and grpc_health_probe
COPY --from=builder --chown=appuser:appuser /workspace/app/build/install/app /app
COPY --from=builder /usr/local/bin/grpc_health_probe /usr/local/bin/grpc_health_probe

# Switch to non-root user
USER 10001:10001

# Cloud Run default PORT environment variable and container port exposure
ENV PORT=8080
EXPOSE 8080

# Preserve graceful shutdown (executes JVM directly as PID 1)
ENTRYPOINT ["/app/bin/app"]
