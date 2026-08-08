package com.codesa.core.controller;

import com.codesa.core.dto.TaskRequest;
import com.codesa.core.dto.TaskResponse;
import com.codesa.core.security.CurrentUser;
import com.codesa.core.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUser currentUser;

    public TaskController(TaskService taskService, CurrentUser currentUser) {
        this.taskService = taskService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request, Authentication auth) {
        TaskResponse response = taskService.create(request, currentUser.userId(auth), currentUser.isAdmin(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> findAll(
            @RequestParam(required = false) Long projectId, Authentication auth) {
        Long userId = currentUser.userId(auth);
        boolean isAdmin = currentUser.isAdmin(auth);

        List<TaskResponse> tasks = projectId != null
                ? taskService.findAllByProject(projectId, userId, isAdmin)
                : taskService.findAll(userId, isAdmin);

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(taskService.findById(id, currentUser.userId(auth), currentUser.isAdmin(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Long id, @Valid @RequestBody TaskRequest request, Authentication auth) {
        return ResponseEntity.ok(
                taskService.update(id, request, currentUser.userId(auth), currentUser.isAdmin(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        taskService.delete(id, currentUser.userId(auth), currentUser.isAdmin(auth));
        return ResponseEntity.noContent().build();
    }
}
