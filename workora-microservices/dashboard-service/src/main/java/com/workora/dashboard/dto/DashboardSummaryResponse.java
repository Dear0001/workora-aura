package com.workora.dashboard.dto;

import java.util.List;
import java.util.Map;

public record DashboardSummaryResponse(
        List<ActivityItem> recentActivity,
        long tasksDueToday,
        long openBugReports,
        long activeProjects,
        long teamMembers,
        long unreadNotifications,
        int phaseCompletion,
        Map<String, Long> taskStatusBreakdown,
        List<TaskActivityPoint> taskActivity,
        List<ActiveProject> activeProjectsList
) {
    public static DashboardSummaryResponse empty() {
        return new DashboardSummaryResponse(
                List.of(), 0, 0, 0, 0, 0, 0,
                Map.of("open", 0L, "inProgress", 0L, "inReview", 0L, "done", 0L),
                List.of(), List.of());
    }

    public record ActivityItem(String id, String type, String message, String occurredAt, String meta) {}
    public record TaskActivityPoint(String date, long completed, long created) {}
    public record ActiveProject(String id, String name, String status, int phaseCompletion,
                                long members, long tasksOpen, long tasksDone, String dueDate, String priority) {}
}
