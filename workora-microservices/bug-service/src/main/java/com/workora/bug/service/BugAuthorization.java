package com.workora.bug.service;

import com.workora.bug.client.OrganizationClient;
import com.workora.bug.client.CallerMembership;
import com.workora.bug.entity.BugReport;
import com.workora.bug.exception.ForbiddenException;
import com.workora.bug.exception.NotFoundException;
import com.workora.bug.security.CurrentUser;
import org.springframework.stereotype.Component;

/**
 * Centralizes resource-level authorization for the Bug domain. Company roles are
 * owned by the Organization service; the report's project manager is denormalized
 * onto the report at creation so that hot path avoids a Project-service call.
 */
@Component
public class BugAuthorization {

    private final OrganizationClient organizationClient;

    public BugAuthorization(OrganizationClient organizationClient) {
        this.organizationClient = organizationClient;
    }

    public boolean isReviewer(BugReport report, CurrentUser caller) {
        CallerMembership membership = activeMembership(report);
        return isReviewer(report, caller, membership);
    }

    public boolean isReviewer(BugReport report, CurrentUser caller, CallerMembership membership) {
        return membership != null && isReviewerRole(report, caller, membership);
    }

    public boolean canViewEvidence(BugReport report, CurrentUser caller) {
        CallerMembership membership = activeMembership(report);
        return membership != null
                && (report.getReporterId().equals(caller.userId()) || isReviewerRole(report, caller, membership));
    }

    public void requireCanViewReport(BugReport report, CurrentUser caller) {
        // 404 (not 403): report existence itself is sensitive to unrelated callers.
        CallerMembership membership = activeMembership(report);
        if (membership == null || (!report.getReporterId().equals(caller.userId())
                && !isReviewerRole(report, caller, membership))) {
            throw new NotFoundException("Bug report not found");
        }
    }

    public void requireCanTriage(BugReport report, CurrentUser caller) {
        if (!isReviewer(report, caller)) {
            throw new ForbiddenException(
                    "Only a company owner/administrator or the project manager can triage this bug report",
                    "BUG_TRIAGE_DENIED");
        }
    }

    public void requireCanViewEvidence(BugReport report, CurrentUser caller) {
        if (!canViewEvidence(report, caller)) {
            throw new ForbiddenException(
                    "Only the reporter or authorized company reviewers can access bug evidence",
                    "EVIDENCE_ACCESS_DENIED");
        }
    }

    private CallerMembership activeMembership(BugReport report) {
        try {
            return organizationClient.myMembership(report.getCompanyId());
        } catch (ForbiddenException | NotFoundException ex) {
            // Hide report existence from callers without active company membership.
            return null;
        }
    }

    private boolean isReviewerRole(BugReport report, CurrentUser caller, CallerMembership membership) {
        return membership.isOwnerOrAdmin()
                || (membership.isProjectManagerOf(report.getDepartmentId())
                && report.getProjectManagerId() != null
                && report.getProjectManagerId().equals(caller.userId()));
    }
}
