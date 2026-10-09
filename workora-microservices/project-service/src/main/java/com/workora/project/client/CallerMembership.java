package com.workora.project.client;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

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

    public boolean isAssignedToDepartment(UUID departmentId) {
        return (departments != null && departmentId != null
                && departments.stream().anyMatch(assignment -> departmentId.equals(assignment.departmentId())))
                || (departmentId != null && departmentId.equals(this.departmentId));
    }

    public List<UUID> departmentIds() {
        List<UUID> ids = new ArrayList<>();
        if (departmentId != null) {
            ids.add(departmentId);
        }
        if (departments != null) {
            departments.stream()
                    .map(DepartmentRoleAssignment::departmentId)
                    .filter(id -> id != null && !ids.contains(id))
                    .forEach(ids::add);
        }
        return List.copyOf(ids);
    }

    private boolean hasDepartmentRole(UUID departmentId, String expectedRole) {
        return departments != null && departmentId != null
                && departments.stream().anyMatch(assignment ->
                departmentId.equals(assignment.departmentId()) && expectedRole.equals(assignment.role()));
    }

    public record DepartmentRoleAssignment(UUID departmentId, String role) {
    }
}
