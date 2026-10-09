package com.workora.bug.dto;

import com.workora.bug.entity.BugReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusChangeRequest(
        @NotNull(message = "Target status is required")
        BugReportStatus status,

        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason
) {
}
