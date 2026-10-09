package com.workora.recruitment.service;

import com.workora.recruitment.client.CallerMembership;
import com.workora.recruitment.client.OrganizationClient;
import com.workora.recruitment.entity.RecruitmentPost;
import com.workora.recruitment.exception.ForbiddenException;
import com.workora.recruitment.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecruitmentAuthorizationTest {

    private final OrganizationClient organizationClient = mock(OrganizationClient.class);
    private final RecruitmentAuthorization authorization = new RecruitmentAuthorization(organizationClient);

    @Test
    void postAuthorMustStillHaveActiveCompanyMembership() {
        UUID companyId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        RecruitmentPost post = post(companyId, authorId);
        when(organizationClient.myMembership(companyId))
                .thenThrow(new ForbiddenException("Company access is suspended", "COMPANY_ACCESS_SUSPENDED"));

        assertThatThrownBy(() -> authorization.requireCanManagePost(
                post, new CurrentUser(authorId, "author@example.test")))
                .isInstanceOf(ForbiddenException.class)
                .extracting(error -> ((ForbiddenException) error).getCode())
                .isEqualTo("COMPANY_ACCESS_SUSPENDED");
    }

    @Test
    void departmentProjectManagerCanManagePostWithDepartmentRole() {
        UUID companyId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        RecruitmentPost post = post(companyId, UUID.randomUUID());
        post.setDepartmentId(departmentId);
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership(
                "DEVELOPER",
                null,
                List.of(new CallerMembership.DepartmentRoleAssignment(departmentId, "PROJECT_MANAGER"))));

        assertThatCode(() -> authorization.requireCanManagePost(
                post, new CurrentUser(managerId, "manager@example.test"))).doesNotThrowAnyException();
    }

    @Test
    void projectManagerCanPublishOnlyForAssignedDepartment() {
        UUID companyId = UUID.randomUUID();
        UUID assignedDepartmentId = UUID.randomUUID();
        UUID otherDepartmentId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership(
                "DEVELOPER",
                null,
                List.of(new CallerMembership.DepartmentRoleAssignment(assignedDepartmentId, "PROJECT_MANAGER"))));
        CurrentUser manager = new CurrentUser(managerId, "manager@example.test");

        assertThatCode(() -> authorization.requireCanPublish(companyId, assignedDepartmentId, manager))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> authorization.requireCanPublish(companyId, otherDepartmentId, manager))
                .isInstanceOf(ForbiddenException.class);
    }

    private RecruitmentPost post(UUID companyId, UUID authorId) {
        RecruitmentPost post = new RecruitmentPost();
        post.setCompanyId(companyId);
        post.setAuthorId(authorId);
        return post;
    }
}
