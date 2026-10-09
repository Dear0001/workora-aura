package com.workora.project.client;

import java.util.UUID;

/**
 * Outbound port to the Organization service. The Project service is the source of
 * truth for projects/phases but must defer company/department membership and role
 * decisions to Organization. Implementations forward the caller's bearer token so
 * Organization authorizes the request as that user.
 */
public interface OrganizationClient {

    /**
     * Resolves the company that owns the given department.
     *
     * @throws com.workora.project.exception.NotFoundException if the department does not
     *         exist or the caller is not a member of its company
     */
    UUID resolveCompanyId(UUID departmentId);

    /**
     * Returns the caller's membership in the given company.
     *
     * @throws com.workora.project.exception.ForbiddenException if the caller is not an
     *         active member of the company
     */
    CallerMembership myMembership(UUID companyId);

    /**
     * Returns an active company member's role and department in the caller's
     * company context.
     */
    CallerMembership member(UUID companyId, UUID userId);
}
