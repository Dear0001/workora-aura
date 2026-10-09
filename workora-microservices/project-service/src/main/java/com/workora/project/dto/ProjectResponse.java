package com.workora.project.dto;

import com.workora.project.entity.ProjectStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID companyId,
        UUID departmentId,
        String name,
        String description,
        UUID projectManagerId,
        LocalDate dueDate,
        int progress,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
