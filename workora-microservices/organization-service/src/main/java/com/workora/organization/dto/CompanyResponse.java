package com.workora.organization.dto;

import com.workora.organization.entity.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String description,
        String contactEmail,
        String contactPhone,
        String address,
        String profileImageUrl,
        String coverImageUrl,
        UUID ownerUserId,
        double averageRating,
        long ratingCount,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
