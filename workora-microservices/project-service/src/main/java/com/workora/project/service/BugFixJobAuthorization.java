package com.workora.project.service;

import com.workora.project.client.OrganizationClient;
import com.workora.project.entity.BugFixJob;
import com.workora.project.entity.BugFixJobStatus;
import com.workora.project.exception.ForbiddenException;
import com.workora.project.exception.NotFoundException;
import com.workora.project.repository.BugFixJobApplicationRepository;
import com.workora.project.security.CurrentUser;
import org.springframework.stereotype.Component;

/**
 * Resource-level authorization for the Bug-Fix Job domain. Company roles are owned by
 * the Organization service, so membership checks defer to it with the caller's token.
 * Freelancers are external (non-members), which is why eligibility is expressed as an
 * inversion of the membership check.
 */
@Component
public class BugFixJobAuthorization {

    private final OrganizationClient organizationClient;
    private final BugFixJobApplicationRepository applicationRepository;

    public BugFixJobAuthorization(OrganizationClient organizationClient,
                                  BugFixJobApplicationRepository applicationRepository) {
        this.organizationClient = organizationClient;
        this.applicationRepository = applicationRepository;
    }

    public void requireCanManageJob(BugFixJob job, CurrentUser caller) {
        if (!canManage(job, caller)) {
            throw new ForbiddenException(
                    "Only the project manager or a company owner/administrator can manage this job",
                    "JOB_MANAGE_DENIED");
        }
    }

    public void requireOwnerOrAdmin(BugFixJob job, CurrentUser caller) {
        if (!isCompanyOwnerOrAdmin(job)) {
            throw new ForbiddenException(
                    "Only a company owner/administrator can approve the fee",
                    "OWNER_APPROVAL_REQUIRED");
        }
    }

    /**
     * Eligibility rule: a user must NOT apply/claim/submit against a job created by their
     * own company. Implemented by inversion — a successful {@code myMembership} return
     * means the caller IS a member of the owning company (ineligible); any exception means
     * they are not a member (eligible).
     */
    public void requireNotOwnCompany(BugFixJob job, CurrentUser caller) {
        try {
            organizationClient.myMembership(job.getCompanyId());
        } catch (RuntimeException notAMember) {
            return; // not a member of the owning company → eligible to apply/submit
        }
        throw new ForbiddenException(
                "You cannot apply or submit to a post created by your company",
                "OWN_COMPANY_ACTION_NOT_ALLOWED");
    }

    public void requireSelectedFreelancer(BugFixJob job, CurrentUser caller) {
        if (job.getSelectedFreelancerId() == null
                || !job.getSelectedFreelancerId().equals(caller.userId())) {
            throw new ForbiddenException(
                    "Only the selected freelancer can perform this action",
                    "NOT_SELECTED_FREELANCER");
        }
    }

    public void requireCanViewJob(BugFixJob job, CurrentUser caller) {
        if (canManage(job, caller)) {
            return;
        }
        if (job.getSelectedFreelancerId() != null
                && job.getSelectedFreelancerId().equals(caller.userId())) {
            return;
        }
        if (applicationRepository.findByJobIdAndApplicantId(job.getId(), caller.userId()).isPresent()) {
            return;
        }
        // An OPEN job is a public advertisement: any authenticated user may view it.
        if (job.getStatus() == BugFixJobStatus.OPEN) {
            return;
        }
        // DRAFT and all other non-public states are visible only to managers/owners.
        throw new NotFoundException("Bug-Fix Job not found");
    }

    private boolean canManage(BugFixJob job, CurrentUser caller) {
        return isProjectManager(job, caller) || isCompanyOwnerOrAdmin(job);
    }

    private boolean isProjectManager(BugFixJob job, CurrentUser caller) {
        return job.getProjectManagerId() != null
                && job.getProjectManagerId().equals(caller.userId());
    }

    private boolean isCompanyOwnerOrAdmin(BugFixJob job) {
        try {
            return organizationClient.myMembership(job.getCompanyId()).isOwnerOrAdmin();
        } catch (RuntimeException notAMember) {
            return false;
        }
    }
}
