package com.workora.dashboard.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.dashboard.dto.AdminDashboardSummaryResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardAggregationServiceTest {
    @Test
    void summarizesCountsFromEachOwningService() {
        DashboardApiClient apiClient = mock(DashboardApiClient.class);
        when(apiClient.identityMetrics("token")).thenReturn(new AdminMetricResponse(11));
        when(apiClient.organizationMetrics("token")).thenReturn(new AdminMetricResponse(12));
        when(apiClient.projectMetrics("token")).thenReturn(new AdminMetricResponse(13));
        when(apiClient.workMetrics("token")).thenReturn(new AdminMetricResponse(14));
        when(apiClient.bugMetrics("token")).thenReturn(new AdminMetricResponse(15));
        when(apiClient.recruitmentMetrics("token")).thenReturn(new AdminMetricResponse(16));
        when(apiClient.notificationMetrics("token")).thenReturn(new AdminMetricResponse(17));
        when(apiClient.paymentMetrics("token")).thenReturn(new AdminMetricResponse(18));

        AdminDashboardSummaryResponse summary =
                new AdminDashboardAggregationService(apiClient).summarize("token");

        assertThat(summary).isEqualTo(new AdminDashboardSummaryResponse(11, 12, 13, 14, 15, 16, 17, 18));
    }
}
