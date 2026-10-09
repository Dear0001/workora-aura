package com.workora.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SelectFreelancerRequest(
        @NotNull(message = "Application id is required")
        UUID applicationId,

        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason
) {
}
