package com.explorewithnk.appcore.controller;

import com.explorewithnk.appcore.model.TaskItem;
import com.explorewithnk.appcore.service.TaskService;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger LOG = Logger.getLogger(TaskController.class);

    @Autowired
    TaskService taskService;

    @GetMapping
    public List<TaskItem> listTasks() {
        LOG.info("REST: Fetching all tasks");
        return taskService.listAllTasks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskItem> getTask(@PathVariable("id") Long id) {
        LOG.infof("REST: Fetching task with ID %d", id);
        TaskItem item = taskService.getTaskById(id);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(item);
    }

    @PostMapping
    public ResponseEntity<TaskItem> createTask(@RequestBody Map<String, String> payload) {
        String title = payload.get("title");
        String description = payload.get("description");

        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        LOG.infof("REST: Creating task with title '%s'", title);
        TaskItem created = taskService.createTask(title, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TaskItem> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") String status) {
        LOG.infof("REST: Updating task ID %d status to '%s'", id, status);
        TaskItem updated = taskService.updateTaskStatus(id, status);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        LOG.infof("REST: Deleting task ID %d", id);
        boolean deleted = taskService.deleteTask(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.noContent().build();
    }
}
