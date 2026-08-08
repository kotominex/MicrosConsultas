package com.codesa.core.dto;

import com.codesa.core.model.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TaskRequest(
        Long projectId,
        @NotBlank(message = "el titulo es obligatorio") String title,
        String description,
        TaskStatus status,
        @NotNull(message = "la fecha de vencimiento es obligatoria") LocalDate dueDate
) {
}
