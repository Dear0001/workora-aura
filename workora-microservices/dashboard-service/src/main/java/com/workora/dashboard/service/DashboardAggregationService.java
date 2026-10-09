package com.workora.dashboard.service;

import com.workora.common.dto.PageResponse;
import com.workora.dashboard.dto.DashboardSummaryResponse;
import com.workora.dashboard.dto.DashboardSummaryResponse.ActivityItem;
import com.workora.dashboard.dto.DashboardSummaryResponse.ActiveProject;
import com.workora.dashboard.dto.DashboardSummaryResponse.TaskActivityPoint;
import com.workora.dashboard.service.DashboardApiClient.BugData;
import com.workora.dashboard.service.DashboardApiClient.CompanyData;
import com.workora.dashboard.service.DashboardApiClient.MemberData;
import com.workora.dashboard.service.DashboardApiClient.PhaseData;
import com.workora.dashboard.service.DashboardApiClient.ProjectData;
import com.workora.dashboard.service.DashboardApiClient.TaskData;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DashboardAggregationService {
    private static final int PAGE_SIZE = 100;
    private static final int PROJECT_LIST_LIMIT = 5;
    private static final int ACTIVITY_LIMIT = 10;
    private static final DateTimeFormatter CHART_DATE = DateTimeFormatter.ofPattern("MMM d");

    private final DashboardApiClient apiClient;

    public DashboardAggregationService(DashboardApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public DashboardSummaryResponse summarize(String token, String userId) {
        LocalDate today = LocalDate.now();
        List<CompanyData> companies = loadAllCompanies(token);
        Set<UUID> teamMemberIds = new HashSet<>();
        List<ProjectMetrics> projectMetrics = new ArrayList<>();
        List<TaskData> allTasks = new ArrayList<>();
        List<BugData> allBugs = new ArrayList<>();
        List<ActivityItem> activities = new ArrayList<>();

        for (CompanyData company : companies) {
            apiClient.companyMembers(token, company.id()).stream()
                    .filter(member -> "ACTIVE".equals(member.status()))
                    .map(MemberData::userId)
                    .filter(id -> id != null && !id.toString().equals(userId))
                    .forEach(teamMemberIds::add);

            for (ProjectData project : loadAllProjects(token, company.id())) {
                List<PhaseData> phases = apiClient.phases(token, project.id());
                Set<UUID> projectMembers = new HashSet<>();
                List<TaskData> projectTasks = new ArrayList<>();
                apiClient.projectMembers(token, project.id()).stream()
                        .map(MemberData::userId)
                        .filter(id -> id != null)
                        .forEach(projectMembers::add);

                int completedPhases = 0;
                for (PhaseData phase : phases) {
                    if ("CLOSED".equals(phase.status()) || "ARCHIVED".equals(phase.status())) {
                        completedPhases++;
                    }
                    List<TaskData> tasks = apiClient.tasks(token, phase.id());
                    List<BugData> bugs = apiClient.bugs(token, phase.id());
                    allTasks.addAll(tasks);
                    projectTasks.addAll(tasks);
                    allBugs.addAll(bugs);
                    tasks.stream()
                            .filter(task -> task.createdAt() != null)
                            .forEach(task -> activities.add(new ActivityItem(
                                    task.id().toString(), "TASK_CREATED", "Task created: " + task.title(),
                                    task.createdAt().toString(), project.name())));
                    bugs.stream()
                            .filter(bug -> bug.createdAt() != null)
                            .forEach(bug -> activities.add(new ActivityItem(
                                    bug.id().toString(), "BUG_REPORTED", "Bug reported: " + bug.title(),
                                    bug.createdAt().toString(), project.name())));
                }

                if (project.createdAt() != null) {
                    activities.add(new ActivityItem(
                            project.id().toString(), "PROJECT_CREATED", "Project created: " + project.name(),
                            project.createdAt().toString(), company.name()));
                }
                projectMetrics.add(new ProjectMetrics(
                        project, phases.size(), completedPhases, projectMembers.size(),
                        projectTasks));
            }
        }

        Map<String, Long> breakdown = taskStatusBreakdown(allTasks);
        List<TaskActivityPoint> taskActivity = taskActivity(allTasks, today);
        List<ActiveProject> activeProjects = projectMetrics.stream()
                .sorted(Comparator.comparing(
                        (ProjectMetrics metrics) -> metrics.project().createdAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(PROJECT_LIST_LIMIT)
                .map(this::toActiveProject)
                .toList();

        long tasksDueToday = allTasks.stream()
                .filter(task -> today.equals(task.dueDate()))
                .filter(task -> !isTaskDone(task.status()))
                .count();
        long openBugs = allBugs.stream()
                .filter(bug -> bug.status() != null
                        && !Set.of("DUPLICATE", "RESOLVED", "REJECTED", "CLOSED").contains(bug.status()))
                .count();
        int totalPhases = projectMetrics.stream().mapToInt(ProjectMetrics::phaseCount).sum();
        int completedPhases = projectMetrics.stream().mapToInt(ProjectMetrics::completedPhaseCount).sum();
        int phaseCompletion = totalPhases == 0
                ? 0
                : (int) Math.round(completedPhases * 100.0 / totalPhases);

        activities.sort(Comparator.comparing(
                ActivityItem::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())));

        return new DashboardSummaryResponse(
                activities.stream().limit(ACTIVITY_LIMIT).toList(),
                tasksDueToday,
                openBugs,
                projectMetrics.size(),
                teamMemberIds.size(),
                apiClient.unreadCount(token).unread(),
                phaseCompletion,
                breakdown,
                taskActivity,
                activeProjects);
    }

    private List<CompanyData> loadAllCompanies(String token) {
        return loadAllPages(page -> apiClient.companies(token, page, PAGE_SIZE));
    }

    private List<ProjectData> loadAllProjects(String token, UUID companyId) {
        return loadAllPages(page -> apiClient.projects(token, companyId, page, PAGE_SIZE));
    }

    private <T> List<T> loadAllPages(java.util.function.IntFunction<PageResponse<T>> fetchPage) {
        List<T> results = new ArrayList<>();
        int pageNumber = 0;
        PageResponse<T> page;
        do {
            page = fetchPage.apply(pageNumber++);
            results.addAll(page.content());
        } while (!page.last());
        return results;
    }

    private Map<String, Long> taskStatusBreakdown(List<TaskData> tasks) {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put("open", tasks.stream().filter(task -> task.status() != null
                && Set.of("DRAFT", "ASSIGNED", "NOT_YET").contains(task.status())).count());
        breakdown.put("inProgress", tasks.stream().filter(task -> "IN_PROGRESS".equals(task.status())).count());
        breakdown.put("inReview", tasks.stream().filter(task -> task.status() != null
                && Set.of("SUBMITTED", "CHANGES_REQUESTED").contains(task.status())).count());
        breakdown.put("done", tasks.stream().filter(task -> isTaskDone(task.status())).count());
        return breakdown;
    }

    private List<TaskActivityPoint> taskActivity(List<TaskData> tasks, LocalDate today) {
        Map<LocalDate, long[]> activity = new HashMap<>();
        for (int daysAgo = 13; daysAgo >= 0; daysAgo--) {
            activity.put(today.minusDays(daysAgo), new long[2]);
        }
        ZoneId zone = ZoneId.systemDefault();
        for (TaskData task : tasks) {
            if (task.createdAt() != null) {
                LocalDate createdDate = task.createdAt().atZone(zone).toLocalDate();
                long[] counts = activity.get(createdDate);
                if (counts != null) counts[1]++;
            }
            if (task.updatedAt() != null && isTaskDone(task.status())) {
                LocalDate completedDate = task.updatedAt().atZone(zone).toLocalDate();
                long[] counts = activity.get(completedDate);
                if (counts != null) counts[0]++;
            }
        }
        return activity.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TaskActivityPoint(
                        entry.getKey().format(CHART_DATE), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private ActiveProject toActiveProject(ProjectMetrics metrics) {
        List<TaskData> tasks = metrics.tasks();
        long done = tasks.stream().filter(task -> isTaskDone(task.status())).count();
        long open = tasks.size() - done;
        LocalDate dueDate = tasks.stream()
                .filter(task -> !isTaskDone(task.status()))
                .map(TaskData::dueDate)
                .filter(date -> date != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new ActiveProject(
                metrics.project().id().toString(),
                metrics.project().name(),
                metrics.project().status(),
                metrics.phaseCompletion(),
                metrics.memberCount(),
                open,
                done,
                dueDate == null ? null : dueDate.toString(),
                null);
    }

    private static boolean isTaskDone(String status) {
        return "ACCEPTED".equals(status) || "COMPLETED".equals(status);
    }

    private record ProjectMetrics(
            ProjectData project,
            int phaseCount,
            int completedPhaseCount,
            int memberCount,
            List<TaskData> tasks) {
        int phaseCompletion() {
            return phaseCount == 0 ? 0 : (int) Math.round(completedPhaseCount * 100.0 / phaseCount);
        }
    }
}
