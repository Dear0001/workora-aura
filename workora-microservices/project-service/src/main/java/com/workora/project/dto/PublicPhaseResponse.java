package com.workora.project.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Public-safe projection of a published public phase for discovery/newsfeed.
 * Deliberately excludes private fields, access links, and member details.
 */
public record PublicPhaseResponse(
        UUID phaseId,
        String name,
        String description,
        UUID projectId,
        String projectName,
        UUID companyId,
        Instant publishedAt
) {
}
