package com.workora.project.dto;

import com.workora.project.entity.FixExpectation;
import com.workora.project.entity.JobCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PATCH semantics: a null field means "leave unchanged". NOTE: fee, currency,
 * fixExpectation and category edits are rejected once a freelancer is assigned
 * (enforced in the service, not here).
 */
public record BugFixJobUpdateRequest(
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 1000) String summary,
        @Size(max = 500) String productType,
        @Size(max = 500) String techStack,
        @Size(max = 500) String versionBuild,
        @Size(max = 500) String environments,
        @Size(max = 4000) String scope,
        @Size(max = 2000) String outOfScope,
        @Size(max = 4000) String authorizedTestingRules,
        @Size(max = 2000) String accessMethod,

        JobCategory category,

        @Size(max = 1000) String severityCriteria,
        @Size(max = 2000) String reproductionRequirements,
        @Size(max = 2000) String evidenceRequirements,

        FixExpectation fixExpectation,

        List<String> requiredSkills,

        @Size(max = 1000) String eligibilityNotes,

        Boolean requiresNda,

        @DecimalMin(value = "0.0", message = "Fee must not be negative")
        BigDecimal fee,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        Boolean splitFeeAllowed,

        @Size(max = 500) String duplicateReportRule,

        Integer maxAcceptedReports,

        Boolean closesAfterOneResolution,

        Instant deadline,

        @Size(max = 2000) String safeHarborStatement,

        UUID taskId
) {
}
