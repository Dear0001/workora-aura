package com.workora.organization.dto;

import com.workora.organization.entity.CompanyRole;
import com.workora.organization.entity.InvitationStatus;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID companyId,
        String email,
        CompanyRole role,
        UUID departmentId,
        InvitationStatus status,
        Instant expiresAt,
        Instant createdAt
) {
}
