package com.workora.payment.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.payment.dto.PaymentResponse;
import com.workora.payment.dto.PaymentVerifyRequest;
import com.workora.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Legacy fee-payment paths kept as aliases for the owner-scoped payment API.
 * They deliberately reuse PaymentService, so they cannot widen payment access.
 */
@RestController
@RequestMapping("/api/v1/fee-payments")
public class FeePaymentCompatibilityController {
    private final PaymentService service;
    public FeePaymentCompatibilityController(PaymentService service) { this.service = service; }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return ApiResponse.success(service.get(user(jwt), paymentId));
    }
    @GetMapping("/{paymentId}/qr")
    public ApiResponse<PaymentResponse> qr(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return ApiResponse.success(service.get(user(jwt), paymentId));
    }
    @PostMapping("/{paymentId}/verify")
    public ApiResponse<PaymentResponse> verify(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
                                               @Valid @RequestBody PaymentVerifyRequest request) {
        return ApiResponse.success("Payment verified", service.verify(user(jwt), paymentId, request));
    }
    @PostMapping("/{paymentId}/confirm-receipt")
    public ApiResponse<PaymentResponse> confirm(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId) {
        return ApiResponse.success("Receipt confirmed", service.confirmReceipt(user(jwt), paymentId));
    }
    @PostMapping("/{paymentId}/dispute")
    public ApiResponse<PaymentResponse> dispute(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
                                                @RequestBody com.workora.payment.dto.PaymentActionRequest request) {
        return ApiResponse.success("Payment dispute opened", service.dispute(user(jwt), paymentId, request.reason()));
    }
}
