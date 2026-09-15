package com.explorewithnk.springapp.controller;

import com.explorewithnk.springapp.repository.TaskRepository;
import com.explorewithnk.springapp.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private static final Logger log = LoggerFactory.getLogger(PublicController.class);

    private final TaskRepository taskRepository;
    private final CacheService cacheService;

    @Value("${spring.datasource.url:jdbc:postgresql://postgresql.default.svc.cluster.local:5432/app_db}")
    private String dbUrl;

    @Value("${spring.data.redis.host:valkey.default.svc.cluster.local}")
    private String redisHost;

    @Value("${spring.kafka.bootstrap-servers:kafka.default.svc.cluster.local:9092}")
    private String kafkaServers;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://keycloak-app.keycloak.svc.cluster.local:8080/realms/master}")
    private String keycloakUrl;

    public PublicController(TaskRepository taskRepository, CacheService cacheService) {
        this.taskRepository = taskRepository;
        this.cacheService = cacheService;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        log.info("HTTP GET /api/public/ping: Health check");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "UP");
        res.put("framework", "Spring Boot " + SpringBootVersion.getVersion());
        res.put("javaVersion", System.getProperty("java.version"));
        res.put("jvmVendor", System.getProperty("java.vendor"));
        res.put("timestamp", Instant.now().toString());

        // Check if OpenTelemetry Java Agent is attached
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean hasOtelAgent = jvmArgs.stream().anyMatch(arg -> arg.contains("opentelemetry-javaagent"));
        res.put("otelJavaAgentAttached", hasOtelAgent);

        return ResponseEntity.ok(res);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getInfo() {
        log.info("HTTP GET /api/public/info: Gathering system information");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", "spring-app");
        info.put("framework", "Spring Boot " + SpringBootVersion.getVersion());
        info.put("javaVersion", System.getProperty("java.version"));

        // PostgreSQL
        Map<String, Object> pg = new LinkedHashMap<>();
        pg.put("url", dbUrl);
        try {
            pg.put("status", "CONNECTED");
            pg.put("taskCount", taskRepository.count());
        } catch (Exception e) {
            pg.put("status", "DEGRADED");
            pg.put("error", e.getMessage());
        }
        info.put("postgresql", pg);

        // Valkey / Redis
        Map<String, Object> valkey = new LinkedHashMap<>();
        valkey.put("host", redisHost);
        valkey.put("status", cacheService.ping() ? "CONNECTED" : "OFFLINE");
        info.put("valkey", valkey);

        // Kafka
        Map<String, Object> kafka = new LinkedHashMap<>();
        kafka.put("bootstrapServers", kafkaServers);
        kafka.put("topic", "task-events");
        kafka.put("status", "CONFIGURED");
        info.put("kafka", kafka);

        // Keycloak
        Map<String, Object> keycloak = new LinkedHashMap<>();
        keycloak.put("issuerUri", keycloakUrl);
        keycloak.put("securedEndpoint", "/api/secure/data");
        info.put("keycloak", keycloak);

        // OpenTelemetry Agent
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        boolean hasOtelAgent = jvmArgs.stream().anyMatch(arg -> arg.contains("opentelemetry-javaagent"));
        Map<String, Object> otel = new LinkedHashMap<>();
        otel.put("instrumentationType", "Zero-Code OpenTelemetry Java Agent");
        otel.put("agentActive", hasOtelAgent);
        otel.put("hardcodedSdkInSource", false);
        otel.put("targetCollector", System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://otel-collector.default.svc.cluster.local:4317"));
        info.put("opentelemetry", otel);

        return ResponseEntity.ok(info);
    }
}
