package com.workora.dashboard.service;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.dashboard.dto.AdminDashboardSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardAggregationService {
    private final DashboardApiClient apiClient;

    public AdminDashboardAggregationService(DashboardApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public AdminDashboardSummaryResponse summarize(String token) {
        return new AdminDashboardSummaryResponse(
                count(apiClient.identityMetrics(token)),
                count(apiClient.organizationMetrics(token)),
                count(apiClient.projectMetrics(token)),
                count(apiClient.workMetrics(token)),
                count(apiClient.bugMetrics(token)),
                count(apiClient.recruitmentMetrics(token)),
                count(apiClient.notificationMetrics(token)),
                count(apiClient.paymentMetrics(token)));
    }

    private long count(AdminMetricResponse metrics) {
        return metrics.totalCount();
    }
}
