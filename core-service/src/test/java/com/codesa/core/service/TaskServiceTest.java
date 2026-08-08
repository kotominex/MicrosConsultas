package com.codesa.core.service;

import com.codesa.core.dto.TaskRequest;
import com.codesa.core.dto.TaskResponse;
import com.codesa.core.exception.ForbiddenAccessException;
import com.codesa.core.exception.InvalidDueDateException;
import com.codesa.core.exception.ProjectArchivedException;
import com.codesa.core.model.Project;
import com.codesa.core.model.ProjectStatus;
import com.codesa.core.model.Task;
import com.codesa.core.model.TaskStatus;
import com.codesa.core.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectService projectService;

    private TaskService taskService;

    private Project activeProject;
    private Project archivedProject;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, projectService);

        activeProject = new Project();
        activeProject.setId(1L);
        activeProject.setOwnerId(10L);
        activeProject.setStatus(ProjectStatus.ACTIVE);

        archivedProject = new Project();
        archivedProject.setId(2L);
        archivedProject.setOwnerId(10L);
        archivedProject.setStatus(ProjectStatus.ARCHIVED);
    }

    @Test
    void create_enProyectoArchivado_lanzaExcepcion() {
        when(projectService.getOwnedProject(2L, 10L, false)).thenReturn(archivedProject);

        TaskRequest request = new TaskRequest(2L, "Tarea", "desc", null, LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> taskService.create(request, 10L, false))
                .isInstanceOf(ProjectArchivedException.class);
    }

    @Test
    void create_conDueDateAnteriorAHoy_lanzaExcepcion() {
        when(projectService.getOwnedProject(1L, 10L, false)).thenReturn(activeProject);

        TaskRequest request = new TaskRequest(1L, "Tarea", "desc", null, LocalDate.now().minusDays(1));

        assertThatThrownBy(() -> taskService.create(request, 10L, false))
                .isInstanceOf(InvalidDueDateException.class);
    }

    @Test
    void create_conDatosValidos_creaTarea() {
        when(projectService.getOwnedProject(1L, 10L, false)).thenReturn(activeProject);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest request = new TaskRequest(1L, "Tarea", "desc", TaskStatus.PENDING, LocalDate.now().plusDays(3));
        TaskResponse response = taskService.create(request, 10L, false);

        assertThat(response.title()).isEqualTo("Tarea");
        assertThat(response.projectId()).isEqualTo(1L);
    }

    @Test
    void findById_usuarioNoDuenioDelProyecto_lanzaForbidden() {
        Task task = new Task();
        task.setId(5L);
        task.setProject(activeProject);
        task.setTitle("Tarea");
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(LocalDate.now().plusDays(1));

        when(taskRepository.findById(5L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.findById(5L, 99L, false))
                .isInstanceOf(ForbiddenAccessException.class);
    }
}
