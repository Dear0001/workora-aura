package com.workora.recruitment.dto;

import com.workora.recruitment.entity.PostStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Safe subset of a recruitment post for public/company-profile listings —
 * carries no company-internal fields and no applicant data.
 */
public record PublicPostResponse(
        UUID id,
        String title,
        String description,
        BigDecimal fee,
        String currency,
        String intendedRole,
        PostStatus status,
        Instant createdAt
) {
}
