package com.workora.bug.dto;

import java.time.Instant;
import java.util.UUID;

public record EvidenceResponse(
        UUID id,
        UUID reportId,
        String fileKey,
        String mimeType,
        String description,
        UUID uploadedBy,
        Instant uploadedAt
) {
}
