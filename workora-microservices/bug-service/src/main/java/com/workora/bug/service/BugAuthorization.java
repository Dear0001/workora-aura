package com.workora.bug.service;

import com.workora.bug.client.OrganizationClient;
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
        if (report.getProjectManagerId() != null && report.getProjectManagerId().equals(caller.userId())) {
            return true;
        }
        try {
            return organizationClient.myMembership(report.getCompanyId()).isOwnerOrAdmin();
        } catch (RuntimeException ex) {
            // A non-member must not learn that the report exists; callers fall back
            // to the 404 path in requireCanViewReport.
            return false;
        }
    }

    public boolean canViewEvidence(BugReport report, CurrentUser caller) {
        return report.getReporterId().equals(caller.userId()) || isReviewer(report, caller);
    }

    public void requireCanViewReport(BugReport report, CurrentUser caller) {
        // 404 (not 403): report existence itself is sensitive to unrelated callers.
        if (!report.getReporterId().equals(caller.userId()) && !isReviewer(report, caller)) {
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
}
