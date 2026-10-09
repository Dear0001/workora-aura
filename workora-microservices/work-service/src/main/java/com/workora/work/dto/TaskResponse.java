package com.workora.work.dto;

import com.workora.work.entity.CompletionRule;
import com.workora.work.entity.TaskStatus;
import com.workora.work.entity.TaskPriority;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID phaseId,
        UUID projectId,
        UUID departmentId,
        UUID companyId,
        UUID projectManagerId,
        String title,
        String description,
        LocalDate dueDate,
        TaskStatus status,
        TaskPriority priority,
        CompletionRule completionRule,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
