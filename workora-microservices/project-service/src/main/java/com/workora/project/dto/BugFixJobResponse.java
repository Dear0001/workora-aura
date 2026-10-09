package com.workora.project.dto;

import com.workora.project.entity.BugFixJobStatus;
import com.workora.project.entity.FixExpectation;
import com.workora.project.entity.JobCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full job view for the project manager, company owner/admin, the selected freelancer
 * and applicants. Includes the private/operational fields (scope, access method, etc.)
 * and the payment seam fields (paymentReference, verifiedAmount, verifiedCurrency) that
 * are NEVER part of the public projection.
 */
public record BugFixJobResponse(
        UUID id,
        UUID phaseId,
        UUID taskId,
        UUID projectId,
        UUID departmentId,
        UUID companyId,
        UUID projectManagerId,
        String title,
        String summary,
        JobCategory category,
        FixExpectation fixExpectation,
        BigDecimal fee,
        String currency,
        BugFixJobStatus status,
        UUID selectedFreelancerId,
        boolean requiresNda,
        Instant deadline,
        List<String> requiredSkills,
        int jobVersion,
        Instant publishedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        String productType,
        String techStack,
        String versionBuild,
        String environments,
        String scope,
        String outOfScope,
        String authorizedTestingRules,
        String accessMethod,
        String severityCriteria,
        String reproductionRequirements,
        String evidenceRequirements,
        String eligibilityNotes,
        String duplicateReportRule,
        Integer maxAcceptedReports,
        boolean closesAfterOneResolution,
        boolean splitFeeAllowed,
        String safeHarborStatement,
        String paymentReference,
        BigDecimal verifiedAmount,
        String verifiedCurrency
) {
}
