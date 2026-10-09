package com.workora.work.dto;

import com.workora.work.entity.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskHistoryResponse(
        UUID id,
        TaskStatus fromStatus,
        TaskStatus toStatus,
        UUID actorId,
        String reason,
        Instant changedAt
) {
}
