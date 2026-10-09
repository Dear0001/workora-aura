package com.workora.recruitment.dto;

import com.workora.recruitment.entity.PostStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecruitmentPostResponse(
        UUID id,
        UUID companyId,
        UUID departmentId,
        UUID projectId,
        String title,
        String description,
        BigDecimal fee,
        String currency,
        String imageUrl,
        String intendedRole,
        UUID authorId,
        PostStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant closedAt
) {
}
