package com.explorewithnk.springapp.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public class TaskEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private Long taskId;
    private String title;
    private String status;
    private String timestamp;

    public TaskEvent() {
    }

    public TaskEvent(String eventType, TaskItem task) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.taskId = task.getId();
        this.title = task.getTitle();
        this.status = task.getStatus();
        this.timestamp = Instant.now().toString();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
