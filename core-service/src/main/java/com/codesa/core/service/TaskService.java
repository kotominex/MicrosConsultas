package com.codesa.core.service;

import com.codesa.core.dto.TaskRequest;
import com.codesa.core.dto.TaskResponse;
import com.codesa.core.exception.ForbiddenAccessException;
import com.codesa.core.exception.InvalidDueDateException;
import com.codesa.core.exception.ProjectArchivedException;
import com.codesa.core.exception.TaskNotFoundException;
import com.codesa.core.model.Project;
import com.codesa.core.model.ProjectStatus;
import com.codesa.core.model.Task;
import com.codesa.core.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    public TaskService(TaskRepository taskRepository, ProjectService projectService) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
    }

    @Transactional
    public TaskResponse create(TaskRequest request, Long userId, boolean isAdmin) {
        Project project = projectService.getOwnedProject(request.projectId(), userId, isAdmin);

        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new ProjectArchivedException(project.getId());
        }
        if (request.dueDate().isBefore(LocalDate.now())) {
            throw new InvalidDueDateException();
        }

        Task task = new Task();
        task.setProject(project);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setDueDate(request.dueDate());

        taskRepository.save(task);
        return TaskResponse.from(task);
    }

    public List<TaskResponse> findAll(Long userId, boolean isAdmin) {
        List<Task> tasks = isAdmin ? taskRepository.findAll() : taskRepository.findAllByProjectOwnerId(userId);
        return tasks.stream().map(TaskResponse::from).toList();
    }

    public List<TaskResponse> findAllByProject(Long projectId, Long userId, boolean isAdmin) {
        projectService.getOwnedProject(projectId, userId, isAdmin);
        return taskRepository.findAllByProjectId(projectId).stream().map(TaskResponse::from).toList();
    }

    public TaskResponse findById(Long id, Long userId, boolean isAdmin) {
        Task task = getOwnedTask(id, userId, isAdmin);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request, Long userId, boolean isAdmin) {
        Task task = getOwnedTask(id, userId, isAdmin);

        if (request.dueDate().isBefore(task.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate())) {
            throw new InvalidDueDateException();
        }

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        if (request.status() != null) {
            task.setStatus(request.status());
        }

        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id, Long userId, boolean isAdmin) {
        Task task = getOwnedTask(id, userId, isAdmin);
        taskRepository.delete(task);
    }

    private Task getOwnedTask(Long id, Long userId, boolean isAdmin) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));

        if (!isAdmin && !task.getProject().getOwnerId().equals(userId)) {
            throw new ForbiddenAccessException();
        }

        return task;
    }
}
