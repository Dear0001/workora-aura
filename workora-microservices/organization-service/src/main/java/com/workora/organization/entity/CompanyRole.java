package com.workora.organization.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Contextual company-scoped role. A user can hold a different role in each company.
 */
public enum CompanyRole {
    OWNER,
    ADMIN,
    PROJECT_MANAGER,
    DEVELOPER,
    TESTER,
    BUG_HUNTER;

    public boolean isOwnerOrAdmin() {
        return this == OWNER || this == ADMIN;
    }

    public Set<CompanyPermission> defaultPermissions() {
        return switch (this) {
            case OWNER -> EnumSet.allOf(CompanyPermission.class);
            case ADMIN -> EnumSet.of(
                    CompanyPermission.MANAGE_COMPANY,
                    CompanyPermission.MANAGE_MEMBERS,
                    CompanyPermission.MANAGE_DEPARTMENTS,
                    CompanyPermission.INVITE_MEMBERS,
                    CompanyPermission.CREATE_PROJECTS,
                    CompanyPermission.VIEW_REPORTS
            );
            case PROJECT_MANAGER -> EnumSet.of(
                    CompanyPermission.CREATE_PROJECTS,
                    CompanyPermission.VIEW_REPORTS,
                    CompanyPermission.MANAGE_DEPARTMENTS
            );
            case DEVELOPER -> EnumSet.of(CompanyPermission.VIEW_REPORTS);
            case TESTER -> EnumSet.of(CompanyPermission.VIEW_REPORTS);
            case BUG_HUNTER -> EnumSet.of(CompanyPermission.VIEW_REPORTS);
        };
    }
}
