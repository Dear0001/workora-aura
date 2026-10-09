package com.workora.project.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProjectUpdateRequest(
        @Size(max = 200, message = "Project name must be at most 200 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        UUID projectManagerId
) {
}
