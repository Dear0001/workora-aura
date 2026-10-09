package com.workora.project.dto;

import com.workora.project.entity.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID companyId,
        UUID departmentId,
        String name,
        String description,
        UUID projectManagerId,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
