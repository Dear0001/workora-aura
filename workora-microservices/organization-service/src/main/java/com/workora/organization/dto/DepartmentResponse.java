package com.workora.organization.dto;

import com.workora.organization.entity.CompanyStatus;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
