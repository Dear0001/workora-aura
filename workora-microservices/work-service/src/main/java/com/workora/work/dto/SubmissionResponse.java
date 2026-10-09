package com.workora.work.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        UUID taskId,
        UUID authorId,
        int version,
        String note,
        List<String> attachmentKeys,
        Instant submittedAt
) {
}
