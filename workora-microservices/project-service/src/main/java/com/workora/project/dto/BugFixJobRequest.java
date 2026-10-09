package com.workora.project.dto;

import com.workora.project.entity.FixExpectation;
import com.workora.project.entity.JobCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BugFixJobRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 1000, message = "Summary must be at most 1000 characters")
        String summary,

        @Size(max = 500) String productType,
        @Size(max = 500) String techStack,
        @Size(max = 500) String versionBuild,
        @Size(max = 500) String environments,
        @Size(max = 4000) String scope,
        @Size(max = 2000) String outOfScope,
        @Size(max = 4000) String authorizedTestingRules,
        @Size(max = 2000) String accessMethod,

        @NotNull(message = "Category is required")
        JobCategory category,

        @Size(max = 1000) String severityCriteria,
        @Size(max = 2000) String reproductionRequirements,
        @Size(max = 2000) String evidenceRequirements,

        @NotNull(message = "Fix expectation is required")
        FixExpectation fixExpectation,

        List<String> requiredSkills,

        @Size(max = 1000) String eligibilityNotes,

        Boolean requiresNda,

        @NotNull(message = "Fee is required")
        @DecimalMin(value = "0.0", message = "Fee must not be negative")
        BigDecimal fee,

        @NotBlank(message = "Currency is required")
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
