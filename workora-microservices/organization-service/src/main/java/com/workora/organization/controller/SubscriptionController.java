package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.organization.dto.SubscriptionResponse;
import com.workora.organization.security.CurrentUser;
import com.workora.organization.service.SubscriptionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscription")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public ApiResponse<SubscriptionResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(subscriptionService.me(CurrentUser.from(jwt)));
    }
}
