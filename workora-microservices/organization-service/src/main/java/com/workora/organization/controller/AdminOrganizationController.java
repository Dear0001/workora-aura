package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.common.dto.PageResponse;
import com.workora.organization.dto.AdminCompanyStatusRequest;
import com.workora.organization.dto.AdminSubscriptionResponse;
import com.workora.organization.dto.AdminSubscriptionUpdateRequest;
import com.workora.organization.dto.CompanyResponse;
import com.workora.organization.entity.MembershipAudit;
import com.workora.organization.service.CompanyService;
import com.workora.organization.service.MembershipService;
import com.workora.organization.service.SubscriptionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrganizationController {
    private final CompanyService companyService;
    private final SubscriptionService subscriptionService;
    private final MembershipService membershipService;

    public AdminOrganizationController(
            CompanyService companyService,
            SubscriptionService subscriptionService,
            MembershipService membershipService) {
        this.companyService = companyService;
        this.subscriptionService = subscriptionService;
        this.membershipService = membershipService;
    }

    @GetMapping("/companies")
    public ApiResponse<PageResponse<CompanyResponse>> companies(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(companyService.adminList(bounded(pageable)));
    }

    @PatchMapping("/companies/{companyId}/status")
    public ApiResponse<CompanyResponse> companyStatus(
            @PathVariable UUID companyId,
            @Valid @RequestBody AdminCompanyStatusRequest request) {
        return ApiResponse.success(companyService.adminSetStatus(companyId, request.status()));
    }

    @GetMapping("/subscriptions")
    public ApiResponse<PageResponse<AdminSubscriptionResponse>> subscriptions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(subscriptionService.adminList(bounded(pageable)));
    }

    @PutMapping("/subscriptions/{userId}")
    public ApiResponse<AdminSubscriptionResponse> updateSubscription(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminSubscriptionUpdateRequest request) {
        return ApiResponse.success(subscriptionService.adminUpdate(userId, request));
    }

    @GetMapping("/activity")
    public ApiResponse<PageResponse<MembershipAudit>> activity(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ApiResponse.success(membershipService.adminActivity(
                companyId, userId, action, from, to, bounded(pageable)));
    }

    private Pageable bounded(Pageable pageable) {
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be between 1 and 100");
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    }
}
