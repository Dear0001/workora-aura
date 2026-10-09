package com.workora.work.dto;

import com.workora.work.entity.AssigneeStatus;

import java.util.UUID;

public record AssigneeProgress(
        UUID userId,
        AssigneeStatus status,
        long submittedCount
) {
}
