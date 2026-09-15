package com.explorewithnk.springapp.service;

import com.explorewithnk.springapp.config.KafkaConfig;
import com.explorewithnk.springapp.model.TaskEvent;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final CacheService cacheService;

    public TaskEventConsumer(ObjectMapper objectMapper, CacheService cacheService) {
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_TASK_EVENTS, groupId = "spring-app-consumer-group")
    public void handleTaskEvent(String message) {
        try {
            TaskEvent event = objectMapper.readValue(message, TaskEvent.class);
            log.info("Kafka Consumer: Received {} event for taskId={} (EventId={})",
                    event.getEventType(), event.getTaskId(), event.getEventId());

            // Check cache or perform follow-up action to generate child span in consumer
            cacheService.getCachedTask(event.getTaskId());

            log.info("Kafka Consumer: Successfully processed event {}", event.getEventId());
        } catch (Exception e) {
            log.error("Kafka Consumer: Error processing message payload: {}", message, e);
        }
    }
}
