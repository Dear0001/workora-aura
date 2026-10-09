package com.workora.work.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskAttachmentResponse(
        UUID id,
        UUID taskId,
        String fileKey,
        String fileName,
        String mimeType,
        String description,
        UUID uploadedBy,
        Instant uploadedAt
) {
}
