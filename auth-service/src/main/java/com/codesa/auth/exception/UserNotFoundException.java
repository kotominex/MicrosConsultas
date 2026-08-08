package com.codesa.auth.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("No se encontro el usuario con id: " + id);
    }
}
