package com.explorewithnk.springapp.service;

import com.explorewithnk.springapp.config.KafkaConfig;
import com.explorewithnk.springapp.model.TaskEvent;
import com.explorewithnk.springapp.model.TaskItem;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TaskEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishTaskEvent(String eventType, TaskItem task) {
        try {
            TaskEvent event = new TaskEvent(eventType, task);
            String payload = objectMapper.writeValueAsString(event);
            String key = String.valueOf(task.getId());

            log.info("Kafka Producer: Emitting {} event for task #{} to topic '{}'", eventType, task.getId(), KafkaConfig.TOPIC_TASK_EVENTS);
            kafkaTemplate.send(KafkaConfig.TOPIC_TASK_EVENTS, key, payload);
        } catch (Exception e) {
            log.error("Kafka Producer: Failed to publish event for task #{}: {}", task.getId(), e.getMessage(), e);
        }
    }
}
