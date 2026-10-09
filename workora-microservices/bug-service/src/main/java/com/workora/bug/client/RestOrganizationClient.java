package com.workora.bug.client;

import com.workora.bug.exception.AppException;
import com.workora.bug.exception.ForbiddenException;
import com.workora.bug.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;

@Component
public class RestOrganizationClient implements OrganizationClient {

    private final RestClient restClient;

    public RestOrganizationClient(@Value("${organization-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public UUID resolveCompanyId(UUID departmentId) {
        record DepartmentData(UUID id, UUID companyId, String name) {
        }
        Envelope<DepartmentData> envelope = restClient.get()
                .uri("/api/v1/departments/{departmentId}", departmentId)
                .headers(this::forwardAuth)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, response) -> {
                            throw new NotFoundException("Department not found");
                        })
                .onStatus(status -> status.is4xxClientError(),
                        (request, response) -> {
                            throw new ForbiddenException(
                                    "Not authorized for this department", "DEPARTMENT_ACCESS_DENIED");
                        })
                .body(new org.springframework.core.ParameterizedTypeReference<Envelope<DepartmentData>>() {
                });
        if (envelope == null || envelope.data() == null || envelope.data().companyId() == null) {
            throw new AppException("Organization service returned an unexpected department payload");
        }
        return envelope.data().companyId();
    }

    @Override
    public CallerMembership myMembership(UUID companyId) {
        record MemberData(String role, UUID departmentId, List<DepartmentData> departmentMemberships) {
        }
        Envelope<MemberData> envelope = restClient.get()
                .uri("/api/v1/companies/{companyId}/members/me", companyId)
                .headers(this::forwardAuth)
                .retrieve()
                .onStatus(status -> status.value() == 404,
                        (request, response) -> {
                            throw new NotFoundException("Company not found");
                        })
                .onStatus(status -> status.is4xxClientError(),
                        (request, response) -> {
                            throw new ForbiddenException(
                                    "You are not a member of this company", "NOT_A_COMPANY_MEMBER");
                        })
                .body(new org.springframework.core.ParameterizedTypeReference<Envelope<MemberData>>() {
                });
        if (envelope == null || envelope.data() == null) {
            throw new AppException("Organization service returned an unexpected membership payload");
        }
        List<CallerMembership.DepartmentRoleAssignment> assignments =
                envelope.data().departmentMemberships() == null ? List.of()
                        : envelope.data().departmentMemberships().stream()
                        .map(department -> new CallerMembership.DepartmentRoleAssignment(
                                department.departmentId(), department.role()))
                        .toList();
        return new CallerMembership(envelope.data().role(), envelope.data().departmentId(), assignments);
    }

    private record DepartmentData(UUID departmentId, String role) {
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
        throw new AppException("Authorization context is required for organization-service calls");
    }

    /** Mirrors the shared {@code ApiResponse} envelope produced by Workora services. */
    record Envelope<T>(boolean success, String message, T data) {
    }
}
