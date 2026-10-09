package com.workora.bug.client;

import java.util.UUID;

/**
 * Outbound port to the Project service. The Bug service stores bug reports but the
 * Project service owns phase/project ownership data. Implementations forward the
 * caller's bearer token so Project authorizes the request as that user.
 */
public interface ProjectClient {

    /**
     * Resolves the full ownership context (project, department, company, project
     * manager) of the given phase.
     *
     * @throws com.workora.bug.exception.NotFoundException if the phase or its project
     *         does not exist or the caller cannot see it
     * @throws com.workora.bug.exception.ForbiddenException if the caller is not
     *         authorized for the phase or its project
     */
    ProjectContext resolvePhase(UUID phaseId);
}
