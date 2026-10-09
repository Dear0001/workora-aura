package com.workora.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PhaseMemberRequest(
        @NotNull(message = "User id is required")
        UUID userId,

        @Email(message = "Email must be valid")
        String email
) {
}
