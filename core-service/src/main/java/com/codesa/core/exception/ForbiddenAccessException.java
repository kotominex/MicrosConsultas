package com.codesa.core.exception;

public class ForbiddenAccessException extends RuntimeException {

    public ForbiddenAccessException() {
        super("No tiene permisos para acceder a este recurso");
    }
}
