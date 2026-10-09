package com.workora.project.mapper;

import com.workora.project.dto.BugFixJobResponse;
import com.workora.project.dto.JobApplicationResponse;
import com.workora.project.dto.JobReviewResponse;
import com.workora.project.dto.JobSubmissionResponse;
import com.workora.project.dto.PhaseMemberResponse;
import com.workora.project.dto.PhaseResponse;
import com.workora.project.dto.ProjectResponse;
import com.workora.project.dto.PublicBugFixJobResponse;
import com.workora.project.entity.BugFixJob;
import com.workora.project.entity.BugFixJobApplication;
import com.workora.project.entity.BugFixJobReview;
import com.workora.project.entity.BugFixJobSubmission;
import com.workora.project.entity.Phase;
import com.workora.project.entity.PhaseMember;
import com.workora.project.entity.Project;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getCompanyId(),
                project.getDepartmentId(),
                project.getName(),
                project.getDescription(),
                project.getProjectManagerId(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public PhaseResponse toResponse(Phase phase) {
        return new PhaseResponse(
                phase.getId(),
                phase.getProjectId(),
                phase.getName(),
                phase.getDescription(),
                phase.getVisibility(),
                phase.getStatus(),
                phase.getCreatedAt(),
                phase.getUpdatedAt(),
                phase.getPublishedAt()
        );
    }

    public PhaseMemberResponse toResponse(PhaseMember member) {
        return new PhaseMemberResponse(
                member.getId(),
                member.getPhaseId(),
                member.getUserId(),
                member.getEmail(),
                member.getCreatedAt()
        );
    }

    public BugFixJobResponse toResponse(BugFixJob job) {
        return new BugFixJobResponse(
                job.getId(),
                job.getPhaseId(),
                job.getTaskId(),
                job.getProjectId(),
                job.getDepartmentId(),
                job.getCompanyId(),
                job.getProjectManagerId(),
                job.getTitle(),
                job.getSummary(),
                job.getCategory(),
                job.getFixExpectation(),
                job.getFee(),
                job.getCurrency(),
                job.getStatus(),
                job.getSelectedFreelancerId(),
                job.isRequiresNda(),
                job.getDeadline(),
                job.getRequiredSkills() != null ? new ArrayList<>(job.getRequiredSkills()) : new ArrayList<>(),
                job.getJobVersion(),
                job.getPublishedAt(),
                job.getClosedAt(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                job.getProductType(),
                job.getTechStack(),
                job.getVersionBuild(),
                job.getEnvironments(),
                job.getScope(),
                job.getOutOfScope(),
                job.getAuthorizedTestingRules(),
                job.getAccessMethod(),
                job.getSeverityCriteria(),
                job.getReproductionRequirements(),
                job.getEvidenceRequirements(),
                job.getEligibilityNotes(),
                job.getDuplicateReportRule(),
                job.getMaxAcceptedReports(),
                job.isClosesAfterOneResolution(),
                job.isSplitFeeAllowed(),
                job.getSafeHarborStatement(),
                job.getPaymentReference(),
                job.getVerifiedAmount(),
                job.getVerifiedCurrency()
        );
    }

    /**
     * Public newsfeed projection. Intentionally omits accessMethod, scope, testing rules,
     * evidence/reproduction requirements, eligibility notes, safe-harbor text, payment
     * fields and applicant data — see {@link PublicBugFixJobResponse}.
     */
    public PublicBugFixJobResponse toPublicResponse(BugFixJob job) {
        return new PublicBugFixJobResponse(
                job.getId(),
                job.getTitle(),
                job.getSummary(),
                job.getCategory(),
                job.getFixExpectation(),
                job.getRequiredSkills() != null ? new ArrayList<>(job.getRequiredSkills()) : new ArrayList<>(),
                job.getFee(),
                job.getCurrency(),
                job.getStatus(),
                job.getCompanyId(),
                job.getProjectId(),
                job.getDeadline(),
                job.getPublishedAt()
        );
    }

    public JobApplicationResponse toResponse(BugFixJobApplication application) {
        return new JobApplicationResponse(
                application.getId(),
                application.getJobId(),
                application.getApplicantId(),
                application.getApplicantEmail(),
                application.getProposal(),
                application.getDecision(),
                application.getCreatedAt(),
                application.getDecidedAt()
        );
    }

    public JobSubmissionResponse toResponse(BugFixJobSubmission submission) {
        return new JobSubmissionResponse(
                submission.getId(),
                submission.getJobId(),
                submission.getFreelancerId(),
                submission.getVersion(),
                submission.getReportSummary(),
                submission.getReproductionDetails(),
                submission.getFixDetails(),
                submission.getEvidenceKeys() != null
                        ? new ArrayList<>(submission.getEvidenceKeys()) : new ArrayList<>(),
                submission.getSubmittedAt()
        );
    }

    public JobReviewResponse toResponse(BugFixJobReview review) {
        return new JobReviewResponse(
                review.getId(),
                review.getJobId(),
                review.getReviewerId(),
                review.getOutcome(),
                review.getReason(),
                review.getTestingEvidenceKeys() != null
                        ? new ArrayList<>(review.getTestingEvidenceKeys()) : new ArrayList<>(),
                review.getCreatedAt()
        );
    }
}
