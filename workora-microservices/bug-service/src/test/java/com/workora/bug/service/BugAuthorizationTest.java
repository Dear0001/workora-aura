package com.workora.bug.service;

import com.workora.bug.client.CallerMembership;
import com.workora.bug.client.OrganizationClient;
import com.workora.bug.entity.BugReport;
import com.workora.bug.exception.ForbiddenException;
import com.workora.bug.exception.NotFoundException;
import com.workora.bug.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BugAuthorizationTest {

    private final OrganizationClient organizationClient = mock(OrganizationClient.class);
    private final BugAuthorization authorization = new BugAuthorization(organizationClient);
    private final UUID companyId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final UUID reporterId = UUID.randomUUID();
    private BugReport report;

    @BeforeEach
    void setUp() {
        report = new BugReport();
        report.setCompanyId(companyId);
        report.setDepartmentId(departmentId);
        report.setReporterId(reporterId);
    }

    @Test
    void reporterCannotViewCompanyReportAfterMembershipAccessIsRevoked() {
        when(organizationClient.myMembership(companyId))
                .thenThrow(new ForbiddenException("Company access is suspended", "COMPANY_ACCESS_SUSPENDED"));

        assertThat(authorization.canViewEvidence(report, new CurrentUser(reporterId, "reporter@example.test")))
                .isFalse();
        assertThatThrownBy(() -> authorization.requireCanViewReport(
                report, new CurrentUser(reporterId, "reporter@example.test")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void departmentProjectManagerCanReviewReportUsingDepartmentRole() {
        UUID managerId = UUID.randomUUID();
        report.setProjectManagerId(managerId);
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership(
                "DEVELOPER",
                null,
                List.of(new CallerMembership.DepartmentRoleAssignment(departmentId, "PROJECT_MANAGER"))));

        assertThat(authorization.isReviewer(report, new CurrentUser(managerId, "manager@example.test"))).isTrue();
    }
}
