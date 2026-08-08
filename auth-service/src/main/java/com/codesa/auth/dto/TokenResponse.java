package com.codesa.auth.dto;

public record TokenResponse(
        String token,
        String tokenType,
        long expiresInMs
) {
    public TokenResponse(String token, long expiresInMs) {
        this(token, "Bearer", expiresInMs);
    }
}
