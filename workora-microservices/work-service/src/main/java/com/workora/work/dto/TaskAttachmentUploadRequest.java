package com.workora.work.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskAttachmentUploadRequest(
        @NotBlank @Size(max = 255) String fileName,
        @Size(max = 255) String contentType
) {
}
