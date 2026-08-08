package com.codesa.core.service;

import com.codesa.core.dto.ProjectRequest;
import com.codesa.core.dto.ProjectResponse;
import com.codesa.core.exception.ForbiddenAccessException;
import com.codesa.core.exception.ProjectNotFoundException;
import com.codesa.core.model.Project;
import com.codesa.core.model.ProjectStatus;
import com.codesa.core.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request, Long ownerId) {
        Project project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStatus(request.status() != null ? request.status() : ProjectStatus.ACTIVE);
        project.setOwnerId(ownerId);

        projectRepository.save(project);
        return ProjectResponse.from(project);
    }

    public List<ProjectResponse> findAll(Long userId, boolean isAdmin) {
        List<Project> projects = isAdmin
                ? projectRepository.findAll()
                : projectRepository.findAllByOwnerId(userId);

        return projects.stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse findById(Long id, Long userId, boolean isAdmin) {
        Project project = getOwnedProject(id, userId, isAdmin);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest request, Long userId, boolean isAdmin) {
        Project project = getOwnedProject(id, userId, isAdmin);

        project.setName(request.name());
        project.setDescription(request.description());
        if (request.status() != null) {
            project.setStatus(request.status());
        }

        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(Long id, Long userId, boolean isAdmin) {
        Project project = getOwnedProject(id, userId, isAdmin);
        projectRepository.delete(project);
    }

    /**
     * Punto unico de resolucion + chequeo de ownership: USER solo accede a lo suyo,
     * ADMIN a todo (ver ARQUITECTURA.md 4.1). Usado por Project y Task para no
     * duplicar la regla de 403.
     */
    Project getOwnedProject(Long id, Long userId, boolean isAdmin) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        if (!isAdmin && !project.getOwnerId().equals(userId)) {
            throw new ForbiddenAccessException();
        }

        return project;
    }
}
