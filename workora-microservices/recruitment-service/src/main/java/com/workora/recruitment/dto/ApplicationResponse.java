package com.workora.recruitment.dto;

import com.workora.recruitment.entity.ApplicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Applicant-facing view: never carries the private reviewer notes.
 */
public record ApplicationResponse(
        UUID id,
        UUID postId,
        UUID applicantId,
        String applicantEmail,
        String coverNote,
        List<String> structuredData,
        ApplicationStatus status,
        Instant submittedAt,
        Instant updatedAt
) {
}
