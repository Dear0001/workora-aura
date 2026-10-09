package com.workora.dashboard.service;

import com.workora.common.dto.ApiResponse;
import com.workora.common.dto.AdminMetricResponse;
import com.workora.common.dto.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class DashboardApiClient {
    private static final ParameterizedTypeReference<ApiResponse<PageResponse<CompanyData>>> COMPANY_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<PageResponse<ProjectData>>> PROJECT_PAGE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<List<MemberData>>> MEMBERS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<List<PhaseData>>> PHASES =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<List<TaskData>>> TASKS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<List<BugData>>> BUGS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<UnreadCountData>> UNREAD_COUNT =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<ApiResponse<AdminMetricResponse>> ADMIN_METRICS =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String organizationUrl;
    private final String projectUrl;
    private final String workUrl;
    private final String bugUrl;
    private final String notificationUrl;
    private final String identityUrl;
    private final String recruitmentUrl;
    private final String paymentUrl;

    public DashboardApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${workora.dashboard.services.organization-url}") String organizationUrl,
            @Value("${workora.dashboard.services.project-url}") String projectUrl,
            @Value("${workora.dashboard.services.work-url}") String workUrl,
            @Value("${workora.dashboard.services.bug-url}") String bugUrl,
            @Value("${workora.dashboard.services.notification-url}") String notificationUrl,
            @Value("${workora.dashboard.services.identity-url}") String identityUrl,
            @Value("${workora.dashboard.services.recruitment-url}") String recruitmentUrl,
            @Value("${workora.dashboard.services.payment-url}") String paymentUrl) {
        this.restClient = restClientBuilder.build();
        this.organizationUrl = organizationUrl;
        this.projectUrl = projectUrl;
        this.workUrl = workUrl;
        this.bugUrl = bugUrl;
        this.notificationUrl = notificationUrl;
        this.identityUrl = identityUrl;
        this.recruitmentUrl = recruitmentUrl;
        this.paymentUrl = paymentUrl;
    }

    public PageResponse<CompanyData> companies(String token, int page, int size) {
        return get(organizationUrl, "/api/v1/companies?page={page}&size={size}", token,
                COMPANY_PAGE, page, size);
    }

    public List<MemberData> companyMembers(String token, UUID companyId) {
        return get(organizationUrl, "/api/v1/companies/{companyId}/members", token,
                MEMBERS, companyId);
    }

    public PageResponse<ProjectData> projects(String token, UUID companyId, int page, int size) {
        return get(projectUrl,
                "/api/v1/companies/{companyId}/projects?page={page}&size={size}", token,
                PROJECT_PAGE, companyId, page, size);
    }

    public List<MemberData> projectMembers(String token, UUID projectId) {
        return get(projectUrl, "/api/v1/projects/{projectId}/members", token,
                MEMBERS, projectId);
    }

    public List<PhaseData> phases(String token, UUID projectId) {
        return get(projectUrl, "/api/v1/projects/{projectId}/phases", token,
                PHASES, projectId);
    }

    public List<TaskData> tasks(String token, UUID phaseId) {
        return get(workUrl, "/api/v1/phases/{phaseId}/tasks", token, TASKS, phaseId);
    }

    public List<BugData> bugs(String token, UUID phaseId) {
        return get(bugUrl, "/api/v1/phases/{phaseId}/bug-reports", token, BUGS, phaseId);
    }

    public UnreadCountData unreadCount(String token) {
        return get(notificationUrl, "/api/v1/notifications/unread-count", token, UNREAD_COUNT);
    }

    public AdminMetricResponse identityMetrics(String token) {
        return adminMetrics(identityUrl, token);
    }

    public AdminMetricResponse organizationMetrics(String token) {
        return adminMetrics(organizationUrl, token);
    }

    public AdminMetricResponse projectMetrics(String token) {
        return adminMetrics(projectUrl, token);
    }

    public AdminMetricResponse workMetrics(String token) {
        return adminMetrics(workUrl, token);
    }

    public AdminMetricResponse bugMetrics(String token) {
        return adminMetrics(bugUrl, token);
    }

    public AdminMetricResponse recruitmentMetrics(String token) {
        return adminMetrics(recruitmentUrl, token);
    }

    public AdminMetricResponse notificationMetrics(String token) {
        return adminMetrics(notificationUrl, token);
    }

    public AdminMetricResponse paymentMetrics(String token) {
        return adminMetrics(paymentUrl, token);
    }

    private AdminMetricResponse adminMetrics(String baseUrl, String token) {
        return get(baseUrl, "/api/v1/admin/metrics", token, ADMIN_METRICS);
    }

    private <T> T get(
            String baseUrl,
            String path,
            String token,
            ParameterizedTypeReference<ApiResponse<T>> responseType,
            Object... uriVariables) {
        ApiResponse<T> response = restClient.get()
                .uri(baseUrl + path, uriVariables)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(responseType);

        if (response == null) {
            throw new IllegalStateException("Dashboard dependency returned an empty response");
        }
        if (!response.success()) {
            throw new IllegalStateException("Dashboard dependency failed: " + response.message());
        }
        if (response.data() == null) {
            throw new IllegalStateException("Dashboard dependency returned no data");
        }
        return response.data();
    }

    public record CompanyData(UUID id, String name, String status) {}
    public record ProjectData(UUID id, String name, String status, Instant createdAt) {}
    public record MemberData(UUID userId, String status) {}
    public record PhaseData(UUID id, String status) {}
    public record TaskData(
            UUID id,
            String title,
            LocalDate dueDate,
            String status,
            Instant createdAt,
            Instant updatedAt) {}
    public record BugData(UUID id, String title, String status, Instant createdAt) {}
    public record UnreadCountData(long unread) {}
}
