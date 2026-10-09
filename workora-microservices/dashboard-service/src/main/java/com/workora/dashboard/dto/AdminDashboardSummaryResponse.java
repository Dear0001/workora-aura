package com.workora.dashboard.dto;

public record AdminDashboardSummaryResponse(
        long accounts,
        long companies,
        long projects,
        long tasks,
        long bugReports,
        long recruitmentPosts,
        long notifications,
        long payments) {
}
