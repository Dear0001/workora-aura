package com.workora.project.dto;

import jakarta.validation.constraints.Size;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectUpdateRequest(
        @Size(max = 200, message = "Project name must be at most 200 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        UUID projectManagerId,

        LocalDate dueDate,

        Boolean clearDueDate,

        @Min(value = 0, message = "Project progress must be at least 0")
        @Max(value = 100, message = "Project progress must not exceed 100")
        Integer progress
) {
}
