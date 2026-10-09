package com.workora.identity.controller;

import com.workora.common.dto.AdminMetricResponse;
import com.workora.common.dto.ApiResponse;
import com.workora.identity.service.AdminMetricsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminMetricsController {
    private final AdminMetricsService adminMetricsService;

    public AdminMetricsController(AdminMetricsService adminMetricsService) {
        this.adminMetricsService = adminMetricsService;
    }

    @GetMapping("/metrics")
    public ApiResponse<AdminMetricResponse> metrics() {
        return ApiResponse.success(adminMetricsService.metrics());
    }
}
