package com.workora.project.service;

import com.workora.project.client.CallerMembership;
import com.workora.project.client.OrganizationClient;
import com.workora.project.entity.Project;
import com.workora.project.exception.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectAuthorizationTest {

    @Mock
    private OrganizationClient organizationClient;

    private final UUID companyId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();
    private final Project project = new Project(companyId, departmentId, "Project", UUID.randomUUID());

    @Test
    void projectManagerCannotArchiveProject() {
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("PROJECT_MANAGER", departmentId));

        assertThatThrownBy(() -> new ProjectAuthorization(organizationClient).requireCanArchiveProject(project))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("PROJECT_ARCHIVE_DENIED");
    }

    @Test
    void projectManagerCannotAssignProjectManager() {
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("PROJECT_MANAGER", departmentId));

        assertThatThrownBy(() ->
                new ProjectAuthorization(organizationClient).requireCanAssignProjectManager(project))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("PROJECT_MANAGER_ASSIGNMENT_DENIED");
    }

    @Test
    void projectManagerCannotChooseProjectManagerWhenCreatingProject() {
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("PROJECT_MANAGER", departmentId));

        assertThatThrownBy(() ->
                new ProjectAuthorization(organizationClient).requireCanAssignProjectManager(companyId))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("PROJECT_MANAGER_ASSIGNMENT_DENIED");
    }

    @Test
    void ownerCanArchiveAndAssignProjectManager() {
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("OWNER", null));
        ProjectAuthorization authorization = new ProjectAuthorization(organizationClient);

        authorization.requireCanArchiveProject(project);
        authorization.requireCanAssignProjectManager(project);

        verify(organizationClient, org.mockito.Mockito.times(2)).myMembership(companyId);
    }
}
