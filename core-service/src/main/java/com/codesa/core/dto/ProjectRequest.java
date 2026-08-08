package com.codesa.core.dto;

import com.codesa.core.model.ProjectStatus;
import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank(message = "el nombre es obligatorio") String name,
        String description,
        ProjectStatus status
) {
}
