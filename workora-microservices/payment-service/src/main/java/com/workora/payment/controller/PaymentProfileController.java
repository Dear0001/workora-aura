package com.workora.payment.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.payment.dto.*;
import com.workora.payment.service.PaymentProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/payment-profiles/bakong")
public class PaymentProfileController {
    private final PaymentProfileService service;
    public PaymentProfileController(PaymentProfileService service) { this.service = service; }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @GetMapping public ApiResponse<PaymentProfileResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.get(user(jwt)));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentProfileResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PaymentProfileRequest request) {
        return ApiResponse.success("Payment profile saved", service.create(user(jwt), request));
    }
    @PatchMapping public ApiResponse<PaymentProfileResponse> update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PaymentProfileRequest request) {
        return ApiResponse.success("Payment profile updated", service.update(user(jwt), request));
    }
    @DeleteMapping public ApiResponse<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        service.delete(user(jwt)); return ApiResponse.success("Payment profile deleted", null);
    }
    @PostMapping("/qr-upload")
    public ApiResponse<PaymentProfileResponse> upload(@AuthenticationPrincipal Jwt jwt, @RequestParam String qrPayload) {
        return ApiResponse.success("QR payload updated", service.uploadQr(user(jwt), qrPayload));
    }
}
