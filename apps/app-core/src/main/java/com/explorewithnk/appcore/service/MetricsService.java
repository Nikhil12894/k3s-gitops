package com.explorewithnk.appcore.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class MetricsService {

    private final Counter tasksCreatedCounter;
    private final Counter kafkaPublishedCounter;
    private final Counter kafkaConsumedCounter;
    private final Counter valkeyHitsCounter;
    private final Counter valkeyMissesCounter;
    private final Timer taskProcessingTimer;

    @Inject
    public MetricsService(Instance<MeterRegistry> registryInstance) {
        MeterRegistry registry = registryInstance.isResolvable() ? registryInstance.get() : Metrics.globalRegistry;

        this.tasksCreatedCounter = Counter.builder("app_tasks_created_total")
                .description("Total number of tasks created in PostgreSQL")
                .register(registry);

        this.kafkaPublishedCounter = Counter.builder("app_kafka_events_published_total")
                .description("Total number of task events published to Kafka")
                .register(registry);

        this.kafkaConsumedCounter = Counter.builder("app_kafka_events_consumed_total")
                .description("Total number of task events consumed from Kafka")
                .register(registry);

        this.valkeyHitsCounter = Counter.builder("app_valkey_cache_hits_total")
                .description("Total number of Valkey cache hits")
                .register(registry);

        this.valkeyMissesCounter = Counter.builder("app_valkey_cache_misses_total")
                .description("Total number of Valkey cache misses")
                .register(registry);

        this.taskProcessingTimer = Timer.builder("app_task_processing_duration_seconds")
                .description("Time taken to process and persist tasks")
                .register(registry);
    }

    public void incrementTasksCreated() {
        tasksCreatedCounter.increment();
    }

    public void incrementKafkaPublished() {
        kafkaPublishedCounter.increment();
    }

    public void incrementKafkaConsumed() {
        kafkaConsumedCounter.increment();
    }

    public void incrementValkeyHit() {
        valkeyHitsCounter.increment();
    }

    public void incrementValkeyMiss() {
        valkeyMissesCounter.increment();
    }

    public Timer getTaskProcessingTimer() {
        return taskProcessingTimer;
    }
}
