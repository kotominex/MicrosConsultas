package com.codesa.core.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(Long id) {
        super("Tarea no encontrada: " + id);
    }
}
