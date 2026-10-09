package com.workora.organization.dto;

import com.workora.organization.entity.CompanyRole;
import com.workora.organization.entity.MembershipStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemberResponse(
        UUID membershipId,
        UUID userId,
        String email,
        CompanyRole role,
        UUID departmentId,
        List<DepartmentRoleAssignmentResponse> departmentMemberships,
        MembershipStatus status,
        Instant createdAt
) {
}
