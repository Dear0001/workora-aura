package com.workora.work.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskAttachmentRequest(
        @NotBlank(message = "fileKey is required")
        @Size(max = 1000, message = "fileKey must be at most 1000 characters")
        String fileKey,

        @Size(max = 255, message = "fileName must be at most 255 characters")
        String fileName,

        @Size(max = 255, message = "mimeType must be at most 255 characters")
        String mimeType,

        @Size(max = 1000, message = "description must be at most 1000 characters")
        String description
) {
}
