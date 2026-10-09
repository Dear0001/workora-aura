package com.workora.bug.client;

import java.util.UUID;

/**
 * Ownership context of a phase as reported by the Project service. Denormalized onto
 * bug reports at creation so reviewer checks never need a cross-service round trip.
 */
public record ProjectContext(
        UUID phaseId,
        UUID projectId,
        UUID departmentId,
        UUID companyId,
        UUID projectManagerId
) {
}
