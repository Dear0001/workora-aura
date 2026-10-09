package com.workora.identity.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.identity.dto.AdminAccessRequestCreate;
import com.workora.identity.entity.AdminAccessRequest;
import com.workora.identity.entity.AdminAccessRequestStatus;
import com.workora.identity.service.AdminAccessRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {
    private final AdminAccessRequestService requestService;

    public AdminAccountController(AdminAccessRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/requests")
    public ApiResponse<List<AdminAccessRequest>> requests(
            @RequestParam(defaultValue = "PENDING") AdminAccessRequestStatus status) {
        return ApiResponse.success(requestService.list(status));
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<AdminAccessRequest>> create(
            Authentication authentication,
            @Valid @RequestBody AdminAccessRequestCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Administrator approval request created",
                requestService.create(request.email(), request.firstName(), request.lastName(), authentication.getName())));
    }

    @PostMapping("/requests/{requestId}/approve")
    public ApiResponse<AdminAccessRequest> approve(
            Authentication authentication,
            @PathVariable UUID requestId) {
        return ApiResponse.success("Administrator access approved",
                requestService.approve(requestId, authentication.getName()));
    }

    @PostMapping("/requests/{requestId}/reject")
    public ApiResponse<AdminAccessRequest> reject(
            Authentication authentication,
            @PathVariable UUID requestId) {
        return ApiResponse.success("Administrator access rejected",
                requestService.reject(requestId, authentication.getName()));
    }
}
