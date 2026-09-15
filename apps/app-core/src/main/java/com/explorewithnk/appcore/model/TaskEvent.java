package com.explorewithnk.appcore.model;

import java.time.Instant;
import java.util.UUID;

public class TaskEvent {

    public String eventId;
    public String eventType;
    public Long taskId;
    public String title;
    public String status;
    public String timestamp;
    public String traceId;

    public TaskEvent() {
    }

    public TaskEvent(String eventType, Long taskId, String title, String status, String traceId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.taskId = taskId;
        this.title = title;
        this.status = status;
        this.timestamp = Instant.now().toString();
        this.traceId = traceId;
    }

    @Override
    public String toString() {
        return "TaskEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", taskId=" + taskId +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", traceId='" + traceId + '\'' +
                '}';
    }
}
