package com.explorewithnk.appcore.controller;

import com.explorewithnk.appcore.model.ClusterInfo;
import com.explorewithnk.appcore.model.TaskItem;
import com.explorewithnk.appcore.service.CacheService;
import io.quarkus.runtime.ImageMode;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @Autowired
    CacheService cacheService;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url", defaultValue = "jdbc:postgresql://postgresql.default.svc.cluster.local:5432/app_db")
    String dbUrl;

    @ConfigProperty(name = "quarkus.datasource.username", defaultValue = "postgres")
    String dbUser;

    @ConfigProperty(name = "quarkus.datasource.password", defaultValue = "")
    String dbPassword;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "kafka.default.svc.cluster.local:9092")
    String kafkaServers;

    @ConfigProperty(name = "quarkus.redis.hosts", defaultValue = "redis://valkey.default.svc.cluster.local:6379")
    String redisHosts;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "http://keycloak-app.keycloak.svc.cluster.local:8080/realms/master")
    String keycloakUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "app-core")
    String keycloakClientId;

    @ConfigProperty(name = "quarkus.otel.exporter.otlp.traces.endpoint", defaultValue = "http://otel-collector.default.svc.cluster.local:4317")
    String otelEndpoint;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "UP");
        res.put("message", "Quarkus Native Microservice is online");
        res.put("timestamp", Instant.now().toString());
        res.put("isNative", ImageMode.current() == ImageMode.NATIVE_RUN);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/info")
    public ResponseEntity<ClusterInfo> getClusterInfo() {
        boolean isNative = ImageMode.current() == ImageMode.NATIVE_RUN;
        String runtimeName = isNative ? "Quarkus GraalVM/Mandrel Native" : "Quarkus JVM (" + System.getProperty("java.version") + ")";

        Map<String, Object> services = new LinkedHashMap<>();

        // 1. PostgreSQL Status
        Map<String, Object> pgInfo = new LinkedHashMap<>();
        pgInfo.put("url", dbUrl);
        pgInfo.put("user", dbUser);
        try {
            long taskCount = TaskItem.count();
            pgInfo.put("status", "CONNECTED");
            pgInfo.put("taskCount", taskCount);
        } catch (Exception e) {
            pgInfo.put("status", "DEGRADED");
            pgInfo.put("error", e.getMessage());
        }
        services.put("postgresql", pgInfo);

        // 2. Valkey / Redis Status
        Map<String, Object> valkeyInfo = new LinkedHashMap<>();
        valkeyInfo.put("host", redisHosts);
        boolean valkeyAlive = cacheService.ping();
        valkeyInfo.put("status", valkeyAlive ? "CONNECTED" : "OFFLINE");
        services.put("valkey", valkeyInfo);

        // 3. Kafka Status
        Map<String, Object> kafkaInfo = new LinkedHashMap<>();
        kafkaInfo.put("bootstrapServers", kafkaServers);
        kafkaInfo.put("status", "CONFIGURED");
        kafkaInfo.put("outgoingTopic", "task-events");
        kafkaInfo.put("incomingTopic", "task-events");
        services.put("kafka", kafkaInfo);

        // 4. Vaultwarden / External Secrets Status
        Map<String, Object> vaultInfo = new LinkedHashMap<>();
        vaultInfo.put("source", "Vaultwarden via ExternalSecrets (ClusterSecretStore/bitwarden-backend)");
        vaultInfo.put("targetSecret", "app-db-secrets");
        vaultInfo.put("passwordInjected", dbPassword != null && !dbPassword.isBlank());
        vaultInfo.put("passwordMasked", maskSecret(dbPassword));
        services.put("vaultwarden", vaultInfo);

        // 5. Keycloak OIDC Status
        Map<String, Object> oidc = new LinkedHashMap<>();
        oidc.put("authServerUrl", keycloakUrl);
        oidc.put("clientId", keycloakClientId);
        oidc.put("securedEndpoint", "/api/secure/data");
        services.put("keycloak", oidc);

        // 6. OpenTelemetry Status
        Map<String, Object> otel = new LinkedHashMap<>();
        otel.put("serviceName", "app-core");
        otel.put("otlpTracesEndpoint", otelEndpoint);
        otel.put("tracesProtocol", "gRPC");
        otel.put("collectorTarget", "otel-collector.default.svc.cluster.local:4317 -> Rootprint UI & Quickwit/MinIO");
        services.put("opentelemetry", otel);

        // 7. Prometheus Metrics Status
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("prometheusEndpoint", "/q/metrics");
        metrics.put("engine", "Micrometer Prometheus");
        services.put("prometheus", metrics);

        ClusterInfo info = new ClusterInfo(
                "app-core",
                "1.0.0-SNAPSHOT",
                runtimeName,
                isNative,
                services
        );

        return ResponseEntity.ok(info);
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return "<not set>";
        }
        if (secret.length() <= 3) {
            return "***";
        }
        return "***" + secret.substring(secret.length() - 3);
    }
}
