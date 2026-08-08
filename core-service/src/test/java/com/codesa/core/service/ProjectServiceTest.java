package com.codesa.core.service;

import com.codesa.core.dto.ProjectRequest;
import com.codesa.core.dto.ProjectResponse;
import com.codesa.core.exception.ForbiddenAccessException;
import com.codesa.core.exception.ProjectNotFoundException;
import com.codesa.core.model.Project;
import com.codesa.core.model.ProjectStatus;
import com.codesa.core.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("Proyecto X");
        project.setStatus(ProjectStatus.ACTIVE);
        project.setOwnerId(10L);
    }

    @Test
    void create_asignaOwnerIdDelUsuarioActual() {
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        ProjectRequest request = new ProjectRequest("Proyecto X", "desc", null);
        ProjectResponse response = projectService.create(request, 10L);

        assertThat(response.name()).isEqualTo("Proyecto X");
    }

    @Test
    void findById_usuarioDuenio_devuelveProyecto() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.findById(1L, 10L, false);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void findById_usuarioNoDuenio_lanzaForbidden() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.findById(1L, 99L, false))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void findById_admin_accedeAUnProyectoAjeno() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.findById(1L, 99L, true);

        assertThat(response.ownerId()).isEqualTo(10L);
    }

    @Test
    void findById_noExiste_lanzaNotFound() {
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(1L, 10L, false))
                .isInstanceOf(ProjectNotFoundException.class);
    }
}
