package com.workora.notification.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.notification.dto.PreferenceResponse;
import com.workora.notification.dto.PreferenceUpsertRequest;
import com.workora.notification.security.CurrentUser;
import com.workora.notification.service.PreferenceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@SecurityRequirement(name = "bearerAuth")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PreferenceResponse>>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(preferenceService.list(CurrentUser.from(jwt))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PreferenceResponse>> upsert(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PreferenceUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Preference saved",
                preferenceService.upsert(CurrentUser.from(jwt), request)));
    }
}
