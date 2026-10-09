package com.workora.recruitment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workora.recruitment.TestJwtConfig;
import com.workora.recruitment.client.CallerMembership;
import com.workora.recruitment.client.OrganizationClient;
import com.workora.recruitment.dto.RecruitmentPostRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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
class RecruitmentControllerIntegrationTest {

    private static final UUID CALLER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID DEPARTMENT_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationClient organizationClient;

    @Test
    void publicListingRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/public/recruitment-posts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicListingReachableByAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/public/recruitment-posts")
                        .with(jwt().jwt(jwt -> jwt.subject(CALLER_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void ownerCanCreateRecruitmentPost() throws Exception {
        when(organizationClient.resolveCompanyId(DEPARTMENT_ID)).thenReturn(COMPANY_ID);
        when(organizationClient.myMembership(COMPANY_ID))
                .thenReturn(new CallerMembership("OWNER", DEPARTMENT_ID));

        RecruitmentPostRequest request = new RecruitmentPostRequest(
                "Senior Designer",
                "Design marketing assets for our launch campaign",
                new BigDecimal("500.00"),
                "USD",
                null,
                "DESIGNER",
                DEPARTMENT_ID,
                null);

        mockMvc.perform(post("/api/v1/recruitment-posts")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(CALLER_ID.toString())
                                .claim("email", "owner@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Senior Designer"))
                .andExpect(jsonPath("$.data.companyId").value(COMPANY_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }
}
