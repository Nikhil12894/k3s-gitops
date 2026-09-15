package com.explorewithnk.appcore.service;

import com.explorewithnk.appcore.model.TaskEvent;
import com.explorewithnk.appcore.model.TaskItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class TaskService {

    private static final Logger LOG = Logger.getLogger(TaskService.class);

    @Inject
    CacheService cacheService;

    @Inject
    MetricsService metricsService;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Channel("task-events-out")
    Emitter<String> taskEventEmitter;

    @Transactional
    public TaskItem createTask(String title, String description) {
        Timer.Sample sample = Timer.start();

        TaskItem task = new TaskItem(title, description);
        task.persist();

        // 1. Cache in Valkey
        cacheService.putTask(task);

        // 2. Publish event to Kafka
        publishEvent("CREATED", task);

        // 3. Update Prometheus metric
        metricsService.incrementTasksCreated();
        sample.stop(metricsService.getTaskProcessingTimer());

        LOG.infof("Created Task #%d: '%s'", task.id, task.title);
        return task;
    }

    public List<TaskItem> listAllTasks() {
        return TaskItem.listAll();
    }

    public TaskItem getTaskById(Long id) {
        // First check Valkey cache
        TaskItem cached = cacheService.getTask(id);
        if (cached != null) {
            return cached;
        }

        // Cache miss -> Query PostgreSQL
        TaskItem dbItem = TaskItem.findById(id);
        if (dbItem != null) {
            cacheService.putTask(dbItem);
        }
        return dbItem;
    }

    @Transactional
    public TaskItem updateTaskStatus(Long id, String newStatus) {
        TaskItem item = TaskItem.findById(id);
        if (item == null) {
            return null;
        }

        item.status = newStatus;
        item.persist();

        // Update cache
        cacheService.putTask(item);

        // Publish update event
        publishEvent("STATUS_UPDATED", item);

        return item;
    }

    @Transactional
    public boolean deleteTask(Long id) {
        TaskItem item = TaskItem.findById(id);
        if (item == null) {
            return false;
        }

        item.delete();

        // Evict from Valkey
        cacheService.evictTask(id);

        // Publish deletion event
        publishEvent("DELETED", item);

        return true;
    }

    private void publishEvent(String eventType, TaskItem item) {
        try {
            String traceId = Span.current().getSpanContext().getTraceId();
            TaskEvent event = new TaskEvent(eventType, item.id, item.title, item.status, traceId);
            String jsonPayload = objectMapper.writeValueAsString(event);

            taskEventEmitter.send(jsonPayload);
            metricsService.incrementKafkaPublished();
            LOG.infof("Published Kafka event [%s] for task #%d (TraceId: %s)", eventType, item.id, traceId);
        } catch (Exception e) {
            LOG.errorf("Failed to emit Kafka event for task #%d: %s", item.id, e.getMessage());
        }
    }
}
