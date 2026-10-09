package com.workora.project.dto;

import com.workora.project.entity.BugFixJobStatus;
import com.workora.project.entity.FixExpectation;
import com.workora.project.entity.JobCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SAFE newsfeed projection of an OPEN bug-fix job. Deliberately excludes every private
 * or operational field: no accessMethod, no scope/outOfScope, no authorizedTestingRules,
 * no evidence/reproduction requirements, no eligibility notes, no safe-harbor text, no
 * payment fields, and no applicant data. Only the discovery-relevant advertising fields
 * are exposed so any authenticated freelancer can browse open work safely.
 */
public record PublicBugFixJobResponse(
        UUID jobId,
        String title,
        String summary,
        JobCategory category,
        FixExpectation fixExpectation,
        List<String> requiredSkills,
        BigDecimal fee,
        String currency,
        BugFixJobStatus status,
        UUID companyId,
        UUID projectId,
        Instant deadline,
        Instant publishedAt
) {
}
