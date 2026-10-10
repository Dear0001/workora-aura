package com.workora.organization.dto;

import com.workora.organization.entity.CompanyRole;

import java.time.Instant;
import java.util.UUID;

public record PendingInvitationResponse(
        UUID id,
        UUID companyId,
        String companyName,
        String email,
        CompanyRole role,
        UUID departmentId,
        Instant expiresAt,
        Instant createdAt
) {
}
