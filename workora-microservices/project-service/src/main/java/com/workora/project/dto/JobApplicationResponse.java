package com.workora.project.dto;

import com.workora.project.entity.JobApplicationDecision;

import java.time.Instant;
import java.util.UUID;

public record JobApplicationResponse(
        UUID id,
        UUID jobId,
        UUID applicantId,
        String applicantEmail,
        String proposal,
        JobApplicationDecision decision,
        Instant createdAt,
        Instant decidedAt
) {
}
