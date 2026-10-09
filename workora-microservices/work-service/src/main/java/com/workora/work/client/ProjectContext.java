package com.workora.work.client;

import java.util.UUID;

/**
 * Ownership chain context resolved from the Project service for a phase. Tasks
 * denormalize these ids onto their own rows so per-request authorization does not
 * need a cross-service round trip.
 */
public record ProjectContext(
        UUID phaseId,
        UUID projectId,
        UUID departmentId,
        UUID companyId,
        UUID projectManagerId
) {
}
