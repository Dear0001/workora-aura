package com.workora.project.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectAssetResponse(
        UUID id,
        UUID projectId,
        UUID phaseId,
        String name,
        String storageKey,
        String contentType,
        String kind,
        UUID uploadedBy,
        Instant createdAt
) {
}
