package com.workora.payment.controller;

import com.workora.common.dto.*;
import com.workora.payment.dto.*;
import com.workora.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody PaymentCreateRequest request) {
        return ApiResponse.success("Payment created", service.create(UUID.fromString(jwt.getSubject()), request));
    }
    @GetMapping
    public ApiResponse<PageResponse<PaymentResponse>> list(@AuthenticationPrincipal Jwt jwt,
                                                           @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(service.list(UUID.fromString(jwt.getSubject()), pageable));
    }
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(service.get(UUID.fromString(jwt.getSubject()), id));
    }
    @PostMapping("/{id}/verify")
    public ApiResponse<PaymentResponse> verify(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                               @Valid @RequestBody PaymentVerifyRequest request) {
        return ApiResponse.success("Payment verified", service.verify(UUID.fromString(jwt.getSubject()), id, request));
    }
    @PostMapping("/{id}/refresh-status")
    public ApiResponse<PaymentResponse> refreshStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Payment status refreshed",
                service.refreshStatus(UUID.fromString(jwt.getSubject()), id));
    }
    @PostMapping("/{id}/confirm-receipt")
    public ApiResponse<PaymentResponse> confirmReceipt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Receipt confirmed",
                service.confirmReceipt(UUID.fromString(jwt.getSubject()), id));
    }
    @PostMapping("/{id}/dispute")
    public ApiResponse<PaymentResponse> dispute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                @Valid @RequestBody PaymentActionRequest request) {
        return ApiResponse.success("Payment dispute opened",
                service.dispute(UUID.fromString(jwt.getSubject()), id, request.reason()));
    }
}
