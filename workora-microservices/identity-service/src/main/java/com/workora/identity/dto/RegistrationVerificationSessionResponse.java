package com.workora.identity.dto;

import java.time.Instant;

public record RegistrationVerificationSessionResponse(
        String status,
        Instant expiresAt
) {
}
