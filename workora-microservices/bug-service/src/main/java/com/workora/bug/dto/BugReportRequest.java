package com.workora.bug.dto;

import com.workora.bug.entity.BugCategory;
import com.workora.bug.entity.BugSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record BugReportRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        @Size(max = 4000, message = "Problem must be at most 4000 characters")
        String problem,

        @Size(max = 500, message = "Location must be at most 500 characters")
        String location,

        BugCategory category,

        BugSeverity severity,

        UUID taskId
) {
}
