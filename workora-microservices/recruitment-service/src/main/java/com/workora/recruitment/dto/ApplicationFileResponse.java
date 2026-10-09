package com.workora.recruitment.dto;

import java.time.Instant;
import java.util.UUID;

public record ApplicationFileResponse(
        UUID id,
        UUID applicationId,
        String fileKey,
        String mimeType,
        Instant uploadedAt
) {
}
