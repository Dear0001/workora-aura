package com.workora.dashboard.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.dashboard.dto.AdminDashboardSummaryResponse;
import com.workora.dashboard.dto.AdminServiceHealthResponse;
import com.workora.dashboard.dto.DashboardSummaryResponse;
import com.workora.dashboard.service.AdminDashboardAggregationService;
import com.workora.dashboard.service.AdminSystemHealthService;
import com.workora.dashboard.service.DashboardAggregationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardAggregationService dashboardAggregationService;
    private final AdminDashboardAggregationService adminDashboardAggregationService;
    private final AdminSystemHealthService adminSystemHealthService;

    public DashboardController(
            DashboardAggregationService dashboardAggregationService,
            AdminDashboardAggregationService adminDashboardAggregationService,
            AdminSystemHealthService adminSystemHealthService) {
        this.dashboardAggregationService = dashboardAggregationService;
        this.adminDashboardAggregationService = adminDashboardAggregationService;
        this.adminSystemHealthService = adminSystemHealthService;
    }

    @GetMapping("/summary")
    ApiResponse<DashboardSummaryResponse> summary(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(dashboardAggregationService.summarize(
                jwt.getTokenValue(), jwt.getSubject()));
    }

    @GetMapping("/admin/summary")
    ApiResponse<AdminDashboardSummaryResponse> adminSummary(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(adminDashboardAggregationService.summarize(jwt.getTokenValue()));
    }

    @GetMapping("/admin/health")
    ApiResponse<List<AdminServiceHealthResponse>> adminHealth() {
        return ApiResponse.success(adminSystemHealthService.checkServices());
    }
}
