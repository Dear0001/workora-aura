package com.workora.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workora.project.TestJwtConfig;
import com.workora.project.client.CallerMembership;
import com.workora.project.client.OrganizationClient;
import com.workora.project.entity.BugFixJob;
import com.workora.project.entity.BugFixJobStatus;
import com.workora.project.entity.FixExpectation;
import com.workora.project.entity.JobCategory;
import com.workora.project.entity.Phase;
import com.workora.project.entity.Project;
import com.workora.project.repository.BugFixJobRepository;
import com.workora.project.repository.PhaseRepository;
import com.workora.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
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
class BugFixJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PhaseRepository phaseRepository;

    @Autowired
    private BugFixJobRepository jobRepository;

    @MockBean
    private OrganizationClient organizationClient;

    private Jwt tokenFor(UUID userId, String email) {
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", userId.toString(), "email", email));
    }

    @Test
    void unauthenticated_publicListing_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/public/bug-fix-jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicListing_isReachableByAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/public/bug-fix-jobs")
                        .with(jwt().jwt(tokenFor(UUID.randomUUID(), "user@any.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @Transactional
    void companyOwner_canCreateJobInPhase() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();

        Project project = projectRepository.saveAndFlush(new Project(companyId, departmentId, "Proj", ownerId));
        Phase phase = phaseRepository.saveAndFlush(new Phase(project.getId(), "Phase 1"));

        when(organizationClient.myMembership(companyId)).thenReturn(new CallerMembership("OWNER", null));

        Map<String, Object> body = new HashMap<>();
        body.put("title", "Fix login bypass");
        body.put("category", "SECURITY");
        body.put("fixExpectation", "PATCH");
        body.put("fee", "250.00");
        body.put("currency", "USD");

        mockMvc.perform(post("/api/v1/phases/" + phase.getId() + "/bug-fix-jobs")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Fix login bypass"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.companyId").value(companyId.toString()))
                .andExpect(jsonPath("$.data.projectId").value(project.getId().toString()))
                .andExpect(jsonPath("$.data.projectManagerId").value(ownerId.toString()));
    }

    @Test
    @Transactional
    void publicProjection_excludesAccessMethod_butIncludesFee() throws Exception {
        UUID companyId = UUID.randomUUID();
        BugFixJob job = new BugFixJob(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                companyId, UUID.randomUUID(), "Public security bug");
        job.setStatus(BugFixJobStatus.OPEN);
        job.setCategory(JobCategory.SECURITY);
        job.setFixExpectation(FixExpectation.PATCH);
        job.setFee(new BigDecimal("250.00"));
        job.setCurrency("USD");
        job.setAccessMethod("SECRET sandbox credentials policy");
        job.setScope("internal scope that must not leak");
        job.setPublishedAt(Instant.now());
        jobRepository.saveAndFlush(job);

        mockMvc.perform(get("/api/v1/public/bug-fix-jobs")
                        .with(jwt().jwt(tokenFor(UUID.randomUUID(), "freelancer@any.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].fee").exists())
                .andExpect(jsonPath("$.data.content[0].title").value("Public security bug"))
                .andExpect(jsonPath("$.data.content[0].accessMethod").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].scope").doesNotExist());
    }
}
