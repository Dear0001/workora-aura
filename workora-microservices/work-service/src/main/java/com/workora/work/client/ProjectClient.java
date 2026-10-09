package com.workora.work.client;

import java.util.UUID;

/**
 * Outbound port to the Project service. The Project service enforces phase/project
 * visibility with the forwarded caller token, so a 403/404 from it means the caller
 * cannot see the phase and that verdict is propagated.
 */
public interface ProjectClient {

    /**
     * Resolves the full ownership chain (project, department, company, project
     * manager) for the given phase.
     *
     * @throws com.workora.work.exception.NotFoundException if the phase or its
     *         project does not exist or is invisible to the caller
     * @throws com.workora.work.exception.ForbiddenException if the caller may not
     *         access the phase or its project
     */
    ProjectContext resolvePhase(UUID phaseId);
}
