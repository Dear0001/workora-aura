package com.workora.project.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobSubmissionResponse(
        UUID id,
        UUID jobId,
        UUID freelancerId,
        int version,
        String reportSummary,
        String reproductionDetails,
        String fixDetails,
        List<String> evidenceKeys,
        Instant submittedAt
) {
}
