package com.workora.dashboard.controller;

import com.workora.dashboard.dto.AdminDashboardSummaryResponse;
import com.workora.dashboard.service.AdminDashboardAggregationService;
import com.workora.dashboard.service.DashboardAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(com.workora.dashboard.config.SecurityConfig.class)
class DashboardAdminAuthorizationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardAggregationService dashboardAggregationService;

    @MockBean
    private AdminDashboardAggregationService adminDashboardAggregationService;

    @Test
    void deniesAuthenticatedNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(adminDashboardAggregationService);
    }

    @Test
    void allowsAdminAndReturnsAggregateSummary() throws Exception {
        when(adminDashboardAggregationService.summarize("admin-token"))
                .thenReturn(new AdminDashboardSummaryResponse(1, 2, 3, 4, 5, 6, 7, 8));

        mockMvc.perform(get("/api/v1/dashboard/admin/summary")
                        .with(jwt().jwt(token -> token.tokenValue("admin-token"))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accounts").value(1))
                .andExpect(jsonPath("$.data.payments").value(8));
    }
}
