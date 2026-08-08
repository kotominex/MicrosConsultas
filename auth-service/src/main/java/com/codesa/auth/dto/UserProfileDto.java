package com.codesa.auth.dto;

import com.codesa.auth.model.Role;

import java.time.Instant;

public record UserProfileDto(
        Long id,
        String email,
        Role role,
        Instant createdAt
) {
}
