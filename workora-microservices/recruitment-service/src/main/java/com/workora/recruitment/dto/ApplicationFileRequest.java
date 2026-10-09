package com.workora.recruitment.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplicationFileRequest(
        @NotBlank String fileKey,
        String mimeType
) {
}
