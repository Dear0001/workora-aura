package com.workora.organization.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workora.organization.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtConfig.class)
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Jwt tokenFor(UUID userId, String email) {
        return new Jwt("test-token", Instant.now(), Instant.now().plusSeconds(300),
                Map.of("alg", "none"),
                Map.of("sub", userId.toString(), "email", email));
    }

    @Test
    void unauthenticatedRequest_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/companies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCompany_makesCreatorOwner_andNonMembersCannotRead() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Acme Corp",
                "description", "Builds things",
                "contactEmail", "hello@acme.test"
        ));

        MvcResult result = mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Acme Corp"))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String companyId = json.path("data").path("id").asText();
        assertThat(companyId).isNotBlank();

        mockMvc.perform(get("/api/v1/companies/" + companyId)
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Acme Corp"));

        UUID strangerId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/companies/" + companyId)
                        .with(jwt().jwt(tokenFor(strangerId, "stranger@other.test"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("NOT_A_COMPANY_MEMBER"));
    }

    @Test
    void createCompany_withBlankName_failsValidation() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of("name", "  "));

        mockMvc.perform(post("/api/v1/companies")
                        .with(jwt().jwt(tokenFor(ownerId, "owner@acme.test")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void freeSubscription_allowsOnlyOneOwnedActiveCompany() throws Exception {
        UUID ownerId = UUID.randomUUID();
        Jwt token = tokenFor(ownerId, "owner@free.test");
        String body = objectMapper.writeValueAsString(Map.of("name", "First company"));

        mockMvc.perform(get("/api/v1/subscription/me").with(jwt().jwt(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plan").value("FREE"))
                .andExpect(jsonPath("$.data.maxCompanies").value(1));
        mockMvc.perform(post("/api/v1/companies").with(jwt().jwt(token))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/companies").with(jwt().jwt(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Second company"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.data.code").value("COMPANY_QUOTA_EXCEEDED"));
    }
}
