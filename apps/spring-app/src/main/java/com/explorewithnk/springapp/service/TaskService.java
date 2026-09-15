package com.explorewithnk.springapp.service;

import com.explorewithnk.springapp.model.TaskItem;
import com.explorewithnk.springapp.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final CacheService cacheService;
    private final TaskEventProducer taskEventProducer;

    public TaskService(TaskRepository taskRepository, CacheService cacheService, TaskEventProducer taskEventProducer) {
        this.taskRepository = taskRepository;
        this.cacheService = cacheService;
        this.taskEventProducer = taskEventProducer;
    }

    @Transactional
    public TaskItem createTask(String title, String description) {
        log.info("Service: Creating new task with title '{}'", title);
        
        // 1. PostgreSQL DB Write (Database Span)
        TaskItem task = new TaskItem(title, description);
        task = taskRepository.save(task);
        log.info("Service: Task saved to PostgreSQL with id={}", task.getId());

        // 2. Valkey Redis Write (Redis Span)
        cacheService.cacheTask(task);

        // 3. Kafka Event Publish (Kafka Producer Span)
        taskEventProducer.publishTaskEvent("CREATED", task);

        return task;
    }

    public List<TaskItem> listAllTasks() {
        log.info("Service: Listing all tasks from PostgreSQL");
        return taskRepository.findAll();
    }

    public Optional<TaskItem> getTaskById(Long id) {
        log.info("Service: Fetching task with id={}", id);

        // 1. Try Valkey cache first (Redis Span)
        TaskItem cached = cacheService.getCachedTask(id);
        if (cached != null) {
            return Optional.of(cached);
        }

        // 2. Fall back to PostgreSQL (Database Span)
        Optional<TaskItem> task = taskRepository.findById(id);
        task.ifPresent(cacheService::cacheTask);
        return task;
    }

    @Transactional
    public Optional<TaskItem> updateTaskStatus(Long id, String status) {
        log.info("Service: Updating task id={} status to '{}'", id, status);
        Optional<TaskItem> optionalTask = taskRepository.findById(id);
        if (optionalTask.isEmpty()) {
            return Optional.empty();
        }

        TaskItem task = optionalTask.get();
        task.setStatus(status);
        task = taskRepository.save(task);

        // Update cache
        cacheService.cacheTask(task);

        // Publish event
        taskEventProducer.publishTaskEvent("STATUS_UPDATED", task);

        return Optional.of(task);
    }

    @Transactional
    public boolean deleteTask(Long id) {
        log.info("Service: Deleting task id={}", id);
        if (!taskRepository.existsById(id)) {
            return false;
        }

        Optional<TaskItem> task = taskRepository.findById(id);
        taskRepository.deleteById(id);
        cacheService.evictTask(id);
        task.ifPresent(t -> taskEventProducer.publishTaskEvent("DELETED", t));

        return true;
    }
}
