package com.codesa.core.controller;

import com.codesa.core.dto.ProjectRequest;
import com.codesa.core.dto.ProjectResponse;
import com.codesa.core.security.CurrentUser;
import com.codesa.core.service.ProjectService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService projectService, CurrentUser currentUser) {
        this.projectService = projectService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request, Authentication auth) {
        ProjectResponse response = projectService.create(request, currentUser.userId(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> findAll(Authentication auth) {
        return ResponseEntity.ok(projectService.findAll(currentUser.userId(auth), currentUser.isAdmin(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> findById(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(projectService.findById(id, currentUser.userId(auth), currentUser.isAdmin(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @PathVariable Long id, @Valid @RequestBody ProjectRequest request, Authentication auth) {
        return ResponseEntity.ok(
                projectService.update(id, request, currentUser.userId(auth), currentUser.isAdmin(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        projectService.delete(id, currentUser.userId(auth), currentUser.isAdmin(auth));
        return ResponseEntity.noContent().build();
    }
}
