package com.explorewithnk.appcore.messaging;

import com.explorewithnk.appcore.model.TaskEvent;
import com.explorewithnk.appcore.service.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TaskEventConsumer {

    private static final Logger LOG = Logger.getLogger(TaskEventConsumer.class);

    @Inject
    MetricsService metricsService;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("task-events-in")
    public void consumeTaskEvent(String payload) {
        String currentTraceId = Span.current().getSpanContext().getTraceId();
        LOG.infof("[Kafka Consumer] Received raw event. Current TraceId=%s, Payload=%s", currentTraceId, payload);

        try {
            TaskEvent event = objectMapper.readValue(payload, TaskEvent.class);
            metricsService.incrementKafkaConsumed();
            LOG.infof("[Kafka Consumer] Processed TaskEvent [Type=%s, TaskId=%d, EventId=%s, Title='%s']",
                    event.eventType, event.taskId, event.eventId, event.title);
        } catch (Exception e) {
            LOG.errorf("Failed to deserialize Kafka task event payload: %s. Error: %s", payload, e.getMessage());
        }
    }
}
