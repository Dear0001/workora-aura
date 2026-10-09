package com.workora.bug.client;

import java.util.UUID;
import java.util.List;

/**
 * The caller's company membership as reported by the Organization service.
 * Role is carried as the raw enum name to avoid a compile-time dependency on the
 * Organization service's types.
 */
public record CallerMembership(String role, UUID departmentId, List<DepartmentRoleAssignment> departments) {

    public CallerMembership(String role, UUID departmentId) {
        this(role, departmentId, List.of());
    }

    public boolean isOwnerOrAdmin() {
        return "OWNER".equals(role) || "ADMIN".equals(role);
    }

    public boolean isProjectManagerOf(UUID departmentId) {
        return (departments != null && departments.stream().anyMatch(assignment ->
                departmentId != null && departmentId.equals(assignment.departmentId())
                        && "PROJECT_MANAGER".equals(assignment.role())))
                || ("PROJECT_MANAGER".equals(role) && departmentId != null
                && departmentId.equals(this.departmentId));
    }

    public record DepartmentRoleAssignment(UUID departmentId, String role) {
    }
}
