package com.workora.project.dto;

import com.workora.project.entity.PhaseStatus;
import com.workora.project.entity.PhaseVisibility;

import java.time.Instant;
import java.util.UUID;

public record PhaseResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        PhaseVisibility visibility,
        PhaseStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt
) {
}
