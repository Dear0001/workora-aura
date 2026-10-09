package com.workora.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 200, message = "Department name must be at most 200 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description
) {
}
