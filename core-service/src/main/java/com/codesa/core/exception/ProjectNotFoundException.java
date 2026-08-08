package com.codesa.core.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Long id) {
        super("Proyecto no encontrado: " + id);
    }
}
