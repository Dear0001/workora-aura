package com.workora.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyRatingResponse(
        UUID id,
        UUID companyId,
        UUID userId,
        int score,
        String review,
        Instant createdAt
) {
}
