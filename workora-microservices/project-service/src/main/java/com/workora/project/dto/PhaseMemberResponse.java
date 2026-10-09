package com.workora.project.dto;

import java.time.Instant;
import java.util.UUID;

public record PhaseMemberResponse(
        UUID id,
        UUID phaseId,
        UUID userId,
        String email,
        Instant createdAt
) {
}
