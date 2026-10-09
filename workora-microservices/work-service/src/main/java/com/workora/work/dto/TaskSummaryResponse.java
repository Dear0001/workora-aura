package com.workora.work.dto;

import com.workora.work.entity.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskSummaryResponse(
        UUID id,
        UUID phaseId,
        UUID projectId,
        String title,
        TaskStatus status,
        long assigneeCount,
        long submissionCount,
        long attachmentCount,
        Instant lastUpdatedAt,
        Instant lastStatusChangedAt,
        UUID latestStatusActorId,
        String latestStatusReason
) {
}
