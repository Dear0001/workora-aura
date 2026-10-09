package com.workora.work.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workora.work.TestJwtConfig;
import com.workora.work.client.CallerMembership;
import com.workora.work.client.OrganizationClient;
import com.workora.work.client.ProjectClient;
import com.workora.work.client.ProjectContext;
import com.workora.work.entity.Task;
import com.workora.work.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

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
        mockMvc.perform(get("/api/v1/tasks/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void projectManager_canCreateTaskInPhase_withDenormalizedIds() throws Exception {
        UUID managerId = UUID.randomUUID();
        UUID phaseId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(projectClient.resolvePhase(phaseId))
                .thenReturn(new ProjectContext(phaseId, projectId, departmentId, companyId, managerId));

        String body = objectMapper.writeValueAsString(Map.of("title", "Write docs"));

        mockMvc.perform(post("/api/v1/phases/" + phaseId + "/tasks")
                        .with(jwt().jwt(tokenFor(managerId, "pm@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Write docs"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.phaseId").value(phaseId.toString()))
                .andExpect(jsonPath("$.data.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.data.departmentId").value(departmentId.toString()))
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.data.projectManagerId").value(managerId.toString()))
                .andExpect(jsonPath("$.data.createdBy").value(managerId.toString()));
    }

    @Test
    void nonAssigneeNonManager_getTask_returnsNotFound() throws Exception {
        UUID outsiderId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID phaseId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        Task task = taskRepository.save(new Task(
                phaseId, UUID.randomUUID(), UUID.randomUUID(), companyId, managerId,
                "Private task", managerId));

        when(organizationClient.myMembership(companyId))
                .thenReturn(new CallerMembership("DEVELOPER", UUID.randomUUID()));

        mockMvc.perform(get("/api/v1/tasks/" + task.getId())
                        .with(jwt().jwt(tokenFor(outsiderId, "dev@acme.test"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
