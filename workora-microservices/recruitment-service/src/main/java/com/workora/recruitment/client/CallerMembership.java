package com.workora.recruitment.client;

import java.util.UUID;

/**
 * The caller's company membership as reported by the Organization service.
 * Role is carried as the raw enum name to avoid a compile-time dependency on the
 * Organization service's types.
 */
public record CallerMembership(String role, UUID departmentId) {

    public boolean isOwnerOrAdmin() {
        return "OWNER".equals(role) || "ADMIN".equals(role);
    }

    public boolean isProjectManagerOf(UUID departmentId) {
        return "PROJECT_MANAGER".equals(role)
                && departmentId != null
                && departmentId.equals(this.departmentId);
    }
}
