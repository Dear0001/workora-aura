package com.workora.bug.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.bug.dto.EvidenceRequest;
import com.workora.bug.dto.EvidenceResponse;
import com.workora.bug.security.CurrentUser;
import com.workora.bug.service.BugEvidenceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bug-reports/{reportId}/evidence")
@SecurityRequirement(name = "bearerAuth")
public class BugEvidenceController {

    private final BugEvidenceService evidenceService;

    public BugEvidenceController(BugEvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EvidenceResponse>> add(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EvidenceRequest request) {
        EvidenceResponse created = evidenceService.add(reportId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evidence added", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EvidenceResponse>>> list(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(evidenceService.list(reportId, CurrentUser.from(jwt))));
    }

    @DeleteMapping("/{evidenceId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID reportId,
            @PathVariable UUID evidenceId,
            @AuthenticationPrincipal Jwt jwt) {
        evidenceService.delete(reportId, evidenceId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success("Evidence deleted", null));
    }
}
