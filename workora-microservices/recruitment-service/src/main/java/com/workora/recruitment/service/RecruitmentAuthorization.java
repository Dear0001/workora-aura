package com.workora.recruitment.service;

import com.workora.recruitment.client.CallerMembership;
import com.workora.recruitment.client.OrganizationClient;
import com.workora.recruitment.entity.RecruitmentPost;
import com.workora.recruitment.exception.ForbiddenException;
import com.workora.recruitment.security.CurrentUser;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralizes resource-level authorization for the Recruitment domain. Company
 * membership and roles are owned by the Organization service, so every check
 * defers to it with the caller's forwarded token.
 */
@Component
public class RecruitmentAuthorization {

    private final OrganizationClient organizationClient;

    public RecruitmentAuthorization(OrganizationClient organizationClient) {
        this.organizationClient = organizationClient;
    }

    public void requireCanPublish(UUID companyId, UUID departmentId, CurrentUser caller) {
        CallerMembership membership = organizationClient.myMembership(companyId);
        boolean isProjectManager = membership.isProjectManagerOf(departmentId);
        if (!membership.isOwnerOrAdmin() && !isProjectManager) {
            throw new ForbiddenException(
                    "Only a company owner/administrator or the department project manager can publish recruitment posts",
                    "POST_PUBLISH_DENIED");
        }
    }

    public void requireCanManagePost(RecruitmentPost post, CurrentUser caller) {
        CallerMembership membership = organizationClient.myMembership(post.getCompanyId());
        boolean author = post.getAuthorId() != null && post.getAuthorId().equals(caller.userId());
        boolean manager = membership.isOwnerOrAdmin() || membership.isProjectManagerOf(post.getDepartmentId());
        if (!author && !manager) {
            throw new ForbiddenException(
                    "Only the post author, a company owner/administrator or the department project manager can manage this post",
                    "POST_MANAGE_DENIED");
        }
    }

    public void requireCanReview(RecruitmentPost post, CurrentUser caller) {
        CallerMembership membership = organizationClient.myMembership(post.getCompanyId());
        boolean author = post.getAuthorId() != null && post.getAuthorId().equals(caller.userId());
        boolean manager = membership.isOwnerOrAdmin() || membership.isProjectManagerOf(post.getDepartmentId());
        if (!author && !manager) {
            throw new ForbiddenException(
                    "Only a company owner/administrator or the department project manager can review applications",
                    "REVIEW_DENIED");
        }
    }

    public void requireOwnerOrAdmin(UUID companyId, CurrentUser caller) {
        CallerMembership membership = organizationClient.myMembership(companyId);
        if (!membership.isOwnerOrAdmin()) {
            throw new ForbiddenException(
                    "Only a company owner or administrator can perform the final approval",
                    "OWNER_APPROVAL_REQUIRED");
        }
    }

}
