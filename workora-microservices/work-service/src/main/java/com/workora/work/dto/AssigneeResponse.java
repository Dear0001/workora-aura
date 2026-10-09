package com.workora.work.dto;

import com.workora.work.entity.AssigneeStatus;

import java.time.Instant;
import java.util.UUID;

public record AssigneeResponse(
        UUID id,
        UUID taskId,
        UUID userId,
        UUID departmentId,
        AssigneeStatus status,
        UUID assignedBy,
        Instant assignedAt,
        Instant removedAt
) {
}
