package com.workora.work.client;

import com.workora.work.exception.AppException;
import com.workora.work.exception.ForbiddenException;
import com.workora.work.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
public class RestProjectClient implements ProjectClient {

    private final RestClient restClient;

    public RestProjectClient(@Value("${project-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public ProjectContext resolvePhase(UUID phaseId) {
        record PhaseData(UUID id, UUID projectId) {
        }
        Envelope<PhaseData> phaseEnvelope = restClient.get()
                .uri("/api/v1/phases/{phaseId}", phaseId)
                .headers(this::forwardAuth)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, response) -> {
                            throw new NotFoundException("Phase not found");
                        })
                .onStatus(status -> status.is4xxClientError(),
                        (request, response) -> {
                            throw new ForbiddenException(
                                    "Not authorized for this phase", "PHASE_ACCESS_DENIED");
                        })
                .body(new org.springframework.core.ParameterizedTypeReference<Envelope<PhaseData>>() {
                });
        if (phaseEnvelope == null || phaseEnvelope.data() == null || phaseEnvelope.data().projectId() == null) {
            throw new AppException("Project service returned an unexpected phase payload");
        }

        record ProjectData(UUID id, UUID companyId, UUID departmentId, UUID projectManagerId) {
        }
        Envelope<ProjectData> projectEnvelope = restClient.get()
                .uri("/api/v1/projects/{projectId}", phaseEnvelope.data().projectId())
                .headers(this::forwardAuth)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, response) -> {
                            throw new NotFoundException("Project not found");
                        })
                .onStatus(status -> status.is4xxClientError(),
                        (request, response) -> {
                            throw new ForbiddenException(
                                    "Not authorized for this project", "PROJECT_ACCESS_DENIED");
                        })
                .body(new org.springframework.core.ParameterizedTypeReference<Envelope<ProjectData>>() {
                });
        if (projectEnvelope == null || projectEnvelope.data() == null || projectEnvelope.data().companyId() == null) {
            throw new AppException("Project service returned an unexpected project payload");
        }

        ProjectData project = projectEnvelope.data();
        return new ProjectContext(
                phaseId,
                project.id(),
                project.departmentId(),
                project.companyId(),
                project.projectManagerId());
    }

    private void forwardAuth(HttpHeaders headers) {
        String authorization = currentAuthorizationHeader();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private String currentAuthorizationHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader(HttpHeaders.AUTHORIZATION);
        }
        throw new AppException("Authorization context is required for project-service calls");
    }

    /** Mirrors the shared {@code ApiResponse} envelope produced by Workora services. */
    record Envelope<T>(boolean success, String message, T data) {
    }
}
