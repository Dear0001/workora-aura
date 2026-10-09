package com.workora.recruitment.client;

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
     * @throws com.workora.recruitment.exception.NotFoundException if the department does not
     *         exist or the caller is not a member of its company
     */
    UUID resolveCompanyId(UUID departmentId);

    /**
     * Returns the caller's membership in the given company.
     *
     * @throws com.workora.recruitment.exception.ForbiddenException if the caller is not an
     *         active member of the company
     */
    CallerMembership myMembership(UUID companyId);
}
