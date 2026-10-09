package com.workora.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JobSubmissionRequest(
        @NotBlank(message = "Report summary is required")
        @Size(max = 4000, message = "Report summary must be at most 4000 characters")
        String reportSummary,

        @Size(max = 4000, message = "Reproduction details must be at most 4000 characters")
        String reproductionDetails,

        @Size(max = 4000, message = "Fix details must be at most 4000 characters")
        String fixDetails,

        List<String> evidenceKeys
) {
}
