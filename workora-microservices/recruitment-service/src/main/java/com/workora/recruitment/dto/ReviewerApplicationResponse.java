package com.workora.recruitment.dto;

import com.workora.recruitment.entity.ApplicationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reviewer view: includes the private manager/owner notes.
 */
public record ReviewerApplicationResponse(
        UUID id,
        UUID postId,
        UUID applicantId,
        String applicantEmail,
        String coverNote,
        List<String> structuredData,
        ApplicationStatus status,
        String managerNote,
        String ownerNote,
        Instant submittedAt,
        Instant updatedAt
) {
}
