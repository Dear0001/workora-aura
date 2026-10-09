package com.workora.organization.dto;

import com.workora.organization.entity.DepartmentRole;

import java.util.UUID;

public record DepartmentRoleAssignmentResponse(UUID departmentId, DepartmentRole role) {
}
