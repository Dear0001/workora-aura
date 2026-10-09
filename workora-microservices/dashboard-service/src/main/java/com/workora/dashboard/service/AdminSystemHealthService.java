package com.workora.dashboard.service;

import com.workora.dashboard.dto.AdminServiceHealthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminSystemHealthService {
    private final RestClient restClient;
    private final List<ServiceEndpoint> services;

    public AdminSystemHealthService(
            @Value("${workora.dashboard.services.identity-url}") String identityUrl,
            @Value("${workora.dashboard.services.organization-url}") String organizationUrl,
            @Value("${workora.dashboard.services.project-url}") String projectUrl,
            @Value("${workora.dashboard.services.work-url}") String workUrl,
            @Value("${workora.dashboard.services.bug-url}") String bugUrl,
            @Value("${workora.dashboard.services.recruitment-url}") String recruitmentUrl,
            @Value("${workora.dashboard.services.notification-url}") String notificationUrl,
            @Value("${workora.dashboard.services.payment-url}") String paymentUrl,
            @Value("${workora.dashboard.service-url:http://localhost:8109}") String dashboardUrl) {
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.services = List.of(
                new ServiceEndpoint("identity", identityUrl),
                new ServiceEndpoint("organization", organizationUrl),
                new ServiceEndpoint("project", projectUrl),
                new ServiceEndpoint("work", workUrl),
                new ServiceEndpoint("bug", bugUrl),
                new ServiceEndpoint("recruitment", recruitmentUrl),
                new ServiceEndpoint("notification", notificationUrl),
                new ServiceEndpoint("payment", paymentUrl),
                new ServiceEndpoint("dashboard", dashboardUrl));
    }

    public List<AdminServiceHealthResponse> checkServices() {
        return services.parallelStream().map(this::check).toList();
    }

    private AdminServiceHealthResponse check(ServiceEndpoint endpoint) {
        Instant checkedAt = Instant.now();
        try {
            Map<?, ?> body = restClient.get()
                    .uri(endpoint.baseUrl() + "/actuator/health")
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                return new AdminServiceHealthResponse(endpoint.name(), "DOWN", checkedAt, Map.of());
            }
            Object rawStatus = body.get("status");
            String status = rawStatus == null ? "UNKNOWN" : String.valueOf(rawStatus);
            return new AdminServiceHealthResponse(endpoint.name(), status, checkedAt, components(body));
        } catch (RestClientException exception) {
            return new AdminServiceHealthResponse(endpoint.name(), "DOWN", checkedAt, Map.of());
        }
    }

    private Map<String, String> components(Map<?, ?> body) {
        Object rawComponents = body.get("components");
        if (!(rawComponents instanceof Map<?, ?> healthComponents)) {
            return Map.of();
        }
        Map<String, String> statuses = new LinkedHashMap<>();
        healthComponents.forEach((name, value) -> {
            if (value instanceof Map<?, ?> component) {
                Object status = component.get("status");
                if (status != null) {
                    statuses.put(String.valueOf(name), String.valueOf(status));
                }
            }
        });
        return Map.copyOf(statuses);
    }

    private record ServiceEndpoint(String name, String baseUrl) {}
}
