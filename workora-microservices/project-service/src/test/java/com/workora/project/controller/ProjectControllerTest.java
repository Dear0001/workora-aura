package com.workora.project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workora.project.TestJwtConfig;
import com.workora.project.client.CallerMembership;
import com.workora.project.client.OrganizationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationClient organizationClient;

    private Jwt tokenFor(UUID userId, String email) {
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", userId.toString(), "email", email));
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/public/phases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void companyOwner_canCreateProjectInDepartment() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(organizationClient.resolveCompanyId(departmentId)).thenReturn(companyId);
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership("OWNER", null));

        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Mobile App",
                "dueDate", "2026-12-31",
                "progress", 40));

        MvcResult result = mockMvc.perform(post("/api/v1/departments/" + departmentId + "/projects")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Mobile App"))
                .andExpect(jsonPath("$.data.dueDate").value("2026-12-31"))
                .andExpect(jsonPath("$.data.progress").value(40))
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.path("data").path("projectManagerId").asText()).isEqualTo(ownerId.toString());
    }

    @Test
    void developer_cannotCreateProjectInDepartment() throws Exception {
        UUID devId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(organizationClient.resolveCompanyId(departmentId)).thenReturn(companyId);
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("DEVELOPER", departmentId));

        String body = objectMapper.writeValueAsString(Map.of("name", "Mobile App"));

        mockMvc.perform(post("/api/v1/departments/" + departmentId + "/projects")
                        .with(jwt().jwt(tokenFor(devId, "dev@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("PROJECT_CREATE_DENIED"));
    }

    @Test
    void projectProgressOutsideRange_failsValidation() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(organizationClient.resolveCompanyId(departmentId)).thenReturn(companyId);
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership("OWNER", null));

        String body = objectMapper.writeValueAsString(Map.of("name", "Invalid Progress", "progress", 101));

        mockMvc.perform(post("/api/v1/departments/" + departmentId + "/projects")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void projectManagerCanUpdateProgressAndClearDeadline() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(organizationClient.resolveCompanyId(departmentId)).thenReturn(companyId);
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership("OWNER", null));
        MvcResult created = mockMvc.perform(post("/api/v1/departments/" + departmentId + "/projects")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Deadline Project",
                                "dueDate", "2026-12-31",
                                "progress", 40))))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/projects/" + projectId)
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "progress", 75,
                                "clearDueDate", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progress").value(75))
                .andExpect(jsonPath("$.data.dueDate").value(nullValue()));
    }

    @Test
    void departmentProjectManager_canCreateProjectInOwnDepartment() throws Exception {
        UUID pmId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(organizationClient.resolveCompanyId(departmentId)).thenReturn(companyId);
        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("PROJECT_MANAGER", departmentId));

        String body = objectMapper.writeValueAsString(Map.of("name", "Backend API"));

        mockMvc.perform(post("/api/v1/departments/" + departmentId + "/projects")
                        .with(jwt().jwt(tokenFor(pmId, "pm@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Backend API"));
    }

    @Test
    void ownerCannotAssignProjectManagerFromAnotherDepartment() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID otherDepartmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(organizationClient.resolveCompanyId(departmentId)).thenReturn(companyId);
        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership("OWNER", null));
        when(organizationClient.member(companyId, managerId))
                .thenReturn(new CallerMembership("PROJECT_MANAGER", otherDepartmentId));

        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Cross Department Project",
                "projectManagerId", managerId));

        mockMvc.perform(post("/api/v1/departments/" + departmentId + "/projects")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("PROJECT_MANAGER_NOT_ELIGIBLE"));
    }

    @Test
    void publicPhaseListing_isReachableByAnyAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/public/phases")
                        .with(jwt().jwt(tokenFor(userId, "user@any.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createProject_withBlankName_failsValidation() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(organizationClient.resolveCompanyId(any())).thenReturn(companyId);
        when(organizationClient.myMembership(any())).thenReturn(new CallerMembership("OWNER", null));

        mockMvc.perform(post("/api/v1/departments/" + departmentId + "/projects")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", " "))))
                .andExpect(status().isBadRequest());
    }
}
