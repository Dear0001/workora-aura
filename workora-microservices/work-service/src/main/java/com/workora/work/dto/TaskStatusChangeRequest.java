package com.workora.work.dto;

import com.workora.work.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskStatusChangeRequest(
        @NotNull TaskStatus status,
        @Size(max = 1000) String reason
) {
}
