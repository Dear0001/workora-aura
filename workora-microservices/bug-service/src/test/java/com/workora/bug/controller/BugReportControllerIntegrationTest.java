package com.workora.bug.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workora.bug.TestJwtConfig;
import com.workora.bug.client.OrganizationClient;
import com.workora.bug.client.ProjectClient;
import com.workora.bug.client.ProjectContext;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
class BugReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectClient projectClient;

    @MockBean
    private OrganizationClient organizationClient;

    private Jwt tokenFor(UUID userId, String email) {
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", userId.toString(), "email", email));
    }

    @Test
    void unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/bug-reports/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUser_canSubmitBugReportAgainstPhase_withoutEvidenceLeak() throws Exception {
        UUID reporterId = UUID.randomUUID();
        UUID phaseId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID projectManagerId = UUID.randomUUID();

        when(projectClient.resolvePhase(phaseId)).thenReturn(
                new ProjectContext(phaseId, projectId, departmentId, companyId, projectManagerId));

        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Login crash",
                "description", "App crashes when submitting the login form",
                "problem", "Cannot log in",
                "location", "/login",
                "category", "FUNCTIONAL",
                "severity", "HIGH"));

        MvcResult result = mockMvc.perform(post("/api/v1/phases/" + phaseId + "/bug-reports")
                        .with(jwt().jwt(tokenFor(reporterId, "dev@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.phaseId").value(phaseId.toString()))
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.data.reporterId").value(reporterId.toString()))
                .andExpect(jsonPath("$.data.evidence").doesNotExist())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.has("evidence")).isFalse();

        String reportId = data.path("id").asText();
        mockMvc.perform(get("/api/v1/bug-reports/" + reportId)
                        .with(jwt().jwt(tokenFor(reporterId, "dev@acme.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.report.title").value("Login crash"))
                .andExpect(jsonPath("$.data.canViewEvidence").value(true))
                .andExpect(jsonPath("$.data.history[0].toStatus").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.report.evidence").doesNotExist());
    }
}
