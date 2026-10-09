package com.workora.dashboard.controller;

import com.workora.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {
    @GetMapping("/health")
    ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("service", "dashboard-service", "status", "UP"));
    }
}
