package com.workora.organization.dto;

import com.workora.organization.entity.CompanyRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeRoleRequest(
        @NotNull(message = "Role is required")
        CompanyRole role,

        UUID departmentId
) {
}
