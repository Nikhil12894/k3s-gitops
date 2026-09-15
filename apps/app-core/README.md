# App-Core Microservice (Quarkus Native + Gradle)

A cloud-native Java microservice built using **Quarkus Native** and **Gradle**, with Spring API compatibility (`@RestController`, `@Autowired`). It seamlessly integrates with the full infrastructure stack deployed on the K3s cluster:

- 🐘 **PostgreSQL**: Transactional entity persistence (`TaskItem`) via Hibernate ORM with Panache.
- ⚡ **Valkey 8 (Redis RESP)**: High-performance caching layer with cache hit/miss tracking.
- 📨 **Apache Kafka (KRaft)**: Event-driven reactive messaging using SmallRye Reactive Messaging (`task-events`).
- 🔐 **Vaultwarden & External Secrets**: Dynamic secret synchronization (`POSTGRES_PASSWORD`) via `ClusterSecretStore/bitwarden-backend`.
- 🔑 **Keycloak OIDC**: JWT bearer token verification, roles validation, and security context extraction.
- 🔭 **OpenTelemetry**: Native distributed tracing exported directly to `otel-collector.default.svc.cluster.local:4317` (viewable in Rootprint UI & Quickwit logs).
- 📊 **Prometheus Metrics**: Custom business metrics exposed at `/q/metrics` and scraped by the Prometheus Operator.

---

## Architecture Overview

```
                          ┌──────────────┐
                          │  Ingress     │ (app.explorewithnk.com)
                          └──────┬───────┘
                                 │
                     ┌───────────▼───────────┐
                     │   app-core (Quarkus)  │
                     │  Native Image (~35MB) │
                     └─┬───────┬───────┬───┬─┘
                       │       │       │   │
        ┌──────────────┘       │       │   └──────────────┐
        ▼                      ▼       ▼                  ▼
┌──────────────┐     ┌───────────┐ ┌───────────┐  ┌──────────────┐
│  PostgreSQL  │     │  Valkey 8 │ │   Kafka   │  │   Keycloak   │
│   (App DB)   │     │  (Cache)  │ │ (Events)  │  │  (OIDC Auth) │
└──────────────┘     └───────────┘ └───────────┘  └──────────────┘
        ▲                                                 ▲
        │                                                 │
┌───────┴──────┐                                  ┌───────┴──────┐
│ Vaultwarden  │ (ExternalSecrets ESO)            │ OpenTelemetry│
│ Secret Sync  │                                  │ OTLP (gRPC)  │
└──────────────┘                                  └──────────────┘
```

---

## API Endpoints

### 1. Public & System Endpoints
- `GET /api/public/ping`: Basic health check returning status, timestamp, and runtime mode (JVM vs. Native).
- `GET /api/public/info`: Detailed connectivity and configuration report across PostgreSQL, Valkey, Kafka, Vaultwarden, Keycloak, OpenTelemetry, and Prometheus.
- `GET /actuator/health`: Spring Boot actuator compatibility endpoint.
- `GET /q/health/live`: Kubernetes liveness probe.
- `GET /q/health/ready`: Kubernetes readiness probe.
- `GET /q/metrics`: Prometheus scrape target.

### 2. Task CRUD Endpoints (PostgreSQL + Valkey + Kafka + OTel)
- `POST /api/tasks`: Create a new task.
  - Body: `{"title": "My Task", "description": "Details"}`
  - Persists in PostgreSQL, stores in Valkey cache, emits `TaskEvent` to Kafka, increments `app_tasks_created_total`.
- `GET /api/tasks`: List all tasks from PostgreSQL.
- `GET /api/tasks/{id}`: Get task by ID (serves from Valkey cache on hit, loads from DB on miss).
- `PUT /api/tasks/{id}/status?status=COMPLETED`: Update status and emit update event to Kafka.
- `DELETE /api/tasks/{id}`: Delete task, evict from Valkey cache, and emit deletion event.

### 3. Secured Endpoints (Keycloak OIDC)
- `GET /api/secure/data`: Requires `Authorization: Bearer <KEYCLOAK_JWT_TOKEN>`. Returns user principal, roles, and token claims.

---

## Local Development & Build

### Prerequisites
- JDK 21+ (e.g. Eclipse Temurin or GraalVM CE 21)
- Gradle 8.8+ (or use `./gradlew`)

### Running in Dev Mode
```bash
./gradlew quarkusDev
```
Dev UI will be available at [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/).

### Running Tests
```bash
./gradlew test
```

### Packaging Fast-JAR (JVM Mode)
```bash
./gradlew build
# Run jar
java -jar build/quarkus-app/quarkus-run.jar
```

### Compiling Native Executable (GraalVM / Mandrel)
```bash
# Direct local native compile (requires local GraalVM/Mandrel installed)
./gradlew build -Dquarkus.native.enabled=true

# Containerized native compile (builds Linux native executable inside container)
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

---

## Docker & Container Images

### 1. Multi-Stage Native Build (Linux AMD64 for K8s)
```bash
docker build -f src/main/docker/Dockerfile.native-multistage -t ghcr.io/nikhil12894/app-core:latest .
```

### 2. Fast JVM Container Build
```bash
./gradlew build -x test
docker build -f src/main/docker/Dockerfile.jvm -t ghcr.io/nikhil12894/app-core:latest .
```

---

## GitOps & Kubernetes Deployment

The app is managed declaratively by Argo CD via `manifests/spring-apps/app-core.yaml`:
- **Deployment**: Configured with memory limits, JVM container options, and OTel auto-injection annotations.
- **ExternalSecret**: Syncs `POSTGRES_PASSWORD` from Vaultwarden into `app-db-secrets`.
- **Service & Ingress**: Exposes app securely at `https://app.explorewithnk.com` with TLS issued by `cert-manager`.
- **ServiceMonitor**: Prometheus scrapes `/q/metrics` every 15 seconds.
- **HPA**: Autoscales pods from 2 to 6 replicas based on CPU utilization.
