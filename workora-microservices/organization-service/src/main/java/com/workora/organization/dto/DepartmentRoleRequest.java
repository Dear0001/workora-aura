package com.workora.organization.dto;

import com.workora.organization.entity.DepartmentRole;
import jakarta.validation.constraints.NotNull;

public record DepartmentRoleRequest(@NotNull DepartmentRole role) {
}
