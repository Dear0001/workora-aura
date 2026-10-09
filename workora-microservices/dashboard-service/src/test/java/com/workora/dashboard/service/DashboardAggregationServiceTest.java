package com.workora.dashboard.service;

import com.workora.common.dto.PageResponse;
import com.workora.dashboard.service.DashboardApiClient.BugData;
import com.workora.dashboard.service.DashboardApiClient.CompanyData;
import com.workora.dashboard.service.DashboardApiClient.MemberData;
import com.workora.dashboard.service.DashboardApiClient.PhaseData;
import com.workora.dashboard.service.DashboardApiClient.ProjectData;
import com.workora.dashboard.service.DashboardApiClient.TaskData;
import com.workora.dashboard.service.DashboardApiClient.UnreadCountData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardAggregationServiceTest {
    private static final String TOKEN = "test-token";
    private static final String USER_ID = "00000000-0000-4000-8000-000000000001";
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final UUID PHASE_ID = UUID.fromString("00000000-0000-4000-8000-000000000004");
    private static final UUID COMPLETED_PHASE_ID = UUID.fromString("00000000-0000-4000-8000-000000000005");

    @Test
    void aggregatesMetricsFromAuthorizedServiceResponses() {
        DashboardApiClient client = mock(DashboardApiClient.class);
        DashboardAggregationService service = new DashboardAggregationService(client);
        Instant now = Instant.now();
        UUID memberId = UUID.fromString("00000000-0000-4000-8000-000000000099");

        when(client.companies(TOKEN, 0, 100)).thenReturn(PageResponse.of(
                List.of(new CompanyData(COMPANY_ID, "Dev Company", "ACTIVE")), 0, 100, 1));
        when(client.companyMembers(TOKEN, COMPANY_ID)).thenReturn(List.of(
                new MemberData(UUID.fromString(USER_ID), "ACTIVE"),
                new MemberData(memberId, "ACTIVE")));
        when(client.projects(TOKEN, COMPANY_ID, 0, 100)).thenReturn(PageResponse.of(
                List.of(new ProjectData(PROJECT_ID, "Dev Project", "ACTIVE", now)), 0, 100, 1));
        when(client.phases(TOKEN, PROJECT_ID)).thenReturn(List.of(
                new PhaseData(PHASE_ID, "PUBLISHED"),
                new PhaseData(COMPLETED_PHASE_ID, "CLOSED")));
        when(client.projectMembers(TOKEN, PROJECT_ID)).thenReturn(List.of(new MemberData(memberId, null)));
        when(client.tasks(TOKEN, PHASE_ID)).thenReturn(List.of(
                new TaskData(UUID.randomUUID(), "Due today", LocalDate.now(), "ASSIGNED", now, now),
                new TaskData(UUID.randomUUID(), "Completed", null, "COMPLETED", now, now),
                new TaskData(UUID.randomUUID(), "Under review", null, "SUBMITTED", now, now)));
        when(client.tasks(TOKEN, COMPLETED_PHASE_ID)).thenReturn(List.of());
        when(client.bugs(TOKEN, PHASE_ID)).thenReturn(List.of(
                new BugData(UUID.randomUUID(), "Open bug", "SUBMITTED", now),
                new BugData(UUID.randomUUID(), "Closed bug", "CLOSED", now)));
        when(client.bugs(TOKEN, COMPLETED_PHASE_ID)).thenReturn(List.of());
        when(client.unreadCount(TOKEN)).thenReturn(new UnreadCountData(4));

        var summary = service.summarize(TOKEN, USER_ID);

        assertEquals(1, summary.tasksDueToday());
        assertEquals(1, summary.openBugReports());
        assertEquals(1, summary.activeProjects());
        assertEquals(1, summary.teamMembers());
        assertEquals(4, summary.unreadNotifications());
        assertEquals(50, summary.phaseCompletion());
        assertEquals(1, summary.taskStatusBreakdown().get("open"));
        assertEquals(1, summary.taskStatusBreakdown().get("inReview"));
        assertEquals(1, summary.taskStatusBreakdown().get("done"));
        assertEquals(14, summary.taskActivity().size());
        assertEquals(2, summary.activeProjectsList().get(0).tasksOpen());
        assertEquals(1, summary.activeProjectsList().get(0).tasksDone());
        assertEquals(50, summary.activeProjectsList().get(0).phaseCompletion());
        assertEquals(6, summary.recentActivity().size());
    }
}
