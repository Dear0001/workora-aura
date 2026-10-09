package com.workora.project.dto;

import com.workora.project.entity.ReviewOutcome;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobReviewResponse(
        UUID id,
        UUID jobId,
        UUID reviewerId,
        ReviewOutcome outcome,
        String reason,
        List<String> testingEvidenceKeys,
        Instant createdAt
) {
}
