package com.workora.project.service;

import com.workora.project.client.CallerMembership;
import com.workora.project.client.OrganizationClient;
import com.workora.project.entity.Project;
import com.workora.project.exception.ForbiddenException;
import com.workora.project.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralizes resource-level authorization for the Project domain. Membership and
 * company roles are owned by the Organization service, so every check defers to it
 * with the caller's forwarded token; a coarse OAuth scope alone is never sufficient.
 */
@Component
public class ProjectAuthorization {

    private final OrganizationClient organizationClient;

    public ProjectAuthorization(OrganizationClient organizationClient) {
        this.organizationClient = organizationClient;
    }

    public CallerMembership requireCompanyMember(UUID companyId) {
        return organizationClient.myMembership(companyId);
    }

    public void requireCanCreateInDepartment(UUID departmentId, UUID companyId) {
        CallerMembership membership = organizationClient.myMembership(companyId);
        if (!membership.isOwnerOrAdmin() && !membership.isProjectManagerOf(departmentId)) {
            throw new ForbiddenException(
                    "Only a company owner/administrator or the department project manager can create projects here",
                    "PROJECT_CREATE_DENIED");
        }
    }

    public void requireEligibleProjectManager(UUID companyId, UUID departmentId, UUID managerId) {
        CallerMembership candidate = organizationClient.member(companyId, managerId);
        if (!candidate.isProjectManagerOf(departmentId)) {
            throw new ForbiddenException(
                    "The project manager must be an active project manager in this department",
                    "PROJECT_MANAGER_NOT_ELIGIBLE");
        }
    }

    public void requireCanManageProject(Project project, CurrentUser caller) {
        CallerMembership membership = organizationClient.myMembership(project.getCompanyId());
        boolean ownerOrAdmin = membership.isOwnerOrAdmin();
        boolean isProjectManager = membership.isProjectManagerOf(project.getDepartmentId())
                && project.getProjectManagerId() != null
                && project.getProjectManagerId().equals(caller.userId());
        if (!ownerOrAdmin && !isProjectManager) {
            throw new ForbiddenException(
                    "Only the project manager or a company owner/administrator can manage this project",
                    "PROJECT_MANAGE_DENIED");
        }
    }

    public void requireCanArchiveProject(Project project) {
        CallerMembership membership = organizationClient.myMembership(project.getCompanyId());
        if (!membership.isOwnerOrAdmin()) {
            throw new ForbiddenException(
                    "Only a company owner or administrator can archive a project",
                    "PROJECT_ARCHIVE_DENIED");
        }
    }

    public void requireCanAssignProjectManager(Project project) {
        requireCanAssignProjectManager(project.getCompanyId());
    }

    public void requireCanAssignProjectManager(UUID companyId) {
        CallerMembership membership = organizationClient.myMembership(companyId);
        if (!membership.isOwnerOrAdmin()) {
            throw new ForbiddenException(
                    "Only a company owner or administrator can assign a project manager",
                    "PROJECT_MANAGER_ASSIGNMENT_DENIED");
        }
    }

    public boolean canViewProject(Project project, CurrentUser caller) {
        CallerMembership membership = organizationClient.myMembership(project.getCompanyId());
        return membership.isOwnerOrAdmin()
                || membership.isAssignedToDepartment(project.getDepartmentId())
                || (membership.isProjectManagerOf(project.getDepartmentId())
                    && project.getProjectManagerId() != null
                    && project.getProjectManagerId().equals(caller.userId()));
    }
}
