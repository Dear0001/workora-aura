package com.workora.identity.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.identity.dto.*;
import com.workora.identity.service.AuthService;
import com.workora.identity.service.RegistrationVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;
    private final RegistrationVerificationService registrationVerificationService;

    public AuthController(AuthService authService,
                          RegistrationVerificationService registrationVerificationService) {
        this.authService = authService;
        this.registrationVerificationService = registrationVerificationService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.register(request)));
    }

    @GetMapping("/auth/registration-verification-sessions/{token}")
    public ResponseEntity<ApiResponse<RegistrationVerificationSessionResponse>> pollRegistrationVerification(
            @PathVariable("token") String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(registrationVerificationService.poll(token)));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<String>> logout(@Valid @RequestBody LogoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.logout(request)));
    }

    @PostMapping("/auth/password-reset-requests")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.requestPasswordReset(request)));
    }

    @PostMapping("/auth/verification-email-requests")
    public ResponseEntity<ApiResponse<String>> requestVerificationEmail(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.requestVerificationEmail(request)));
    }

    @PostMapping("/auth/password-resets")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(request)));
    }
}
