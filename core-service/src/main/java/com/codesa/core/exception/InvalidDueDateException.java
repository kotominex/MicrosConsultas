package com.codesa.core.exception;

public class InvalidDueDateException extends RuntimeException {

    public InvalidDueDateException() {
        super("La fecha de vencimiento no puede ser anterior a la fecha de creacion de la tarea");
    }
}
