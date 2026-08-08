package com.codesa.core.exception;

public class ProjectArchivedException extends RuntimeException {

    public ProjectArchivedException(Long projectId) {
        super("No se pueden crear tareas en el proyecto archivado: " + projectId);
    }
}
