package com.workora.project.dto;

import com.workora.project.entity.ReviewOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JobReviewRequest(
        @NotNull(message = "Review outcome is required")
        ReviewOutcome outcome,

        @Size(max = 2000, message = "Reason must be at most 2000 characters")
        String reason,

        List<String> testingEvidenceKeys
) {
}
