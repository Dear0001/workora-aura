package com.workora.work.client;

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
        return hasDepartmentRole(departmentId, "PROJECT_MANAGER")
                || ("PROJECT_MANAGER".equals(role) && departmentId != null && departmentId.equals(this.departmentId));
    }

    public boolean isDeveloperOrTesterIn(UUID departmentId) {
        return hasDepartmentRole(departmentId, "DEVELOPER", "TESTER")
                || (("DEVELOPER".equals(role) || "TESTER".equals(role))
                && departmentId != null && departmentId.equals(this.departmentId));
    }

    public String roleIn(UUID departmentId) {
        if (departments != null && departmentId != null) {
            return departments.stream()
                    .filter(assignment -> departmentId.equals(assignment.departmentId()))
                    .map(DepartmentRoleAssignment::role)
                    .findFirst()
                    .orElseGet(() -> departmentId.equals(this.departmentId) ? role : null);
        }
        return departmentId != null && departmentId.equals(this.departmentId) ? role : null;
    }

    private boolean hasDepartmentRole(UUID departmentId, String... expectedRoles) {
        return departments != null && departmentId != null
                && departments.stream().anyMatch(assignment ->
                departmentId.equals(assignment.departmentId())
                        && java.util.Arrays.asList(expectedRoles).contains(assignment.role()));
    }

    public record DepartmentRoleAssignment(UUID departmentId, String role) {
    }
}
