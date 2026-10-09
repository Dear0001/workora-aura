package com.workora.bug.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.bug.dto.BugReportDetailResponse;
import com.workora.bug.dto.BugReportRequest;
import com.workora.bug.dto.BugReportResponse;
import com.workora.bug.dto.StatusChangeRequest;
import com.workora.bug.security.CurrentUser;
import com.workora.bug.service.BugReportService;
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
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class BugReportController {

    private final BugReportService bugReportService;

    public BugReportController(BugReportService bugReportService) {
        this.bugReportService = bugReportService;
    }

    @PostMapping("/phases/{phaseId}/bug-reports")
    public ResponseEntity<ApiResponse<BugReportResponse>> submit(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BugReportRequest request) {
        BugReportResponse created = bugReportService.submit(phaseId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bug report submitted", created));
    }

    @GetMapping("/phases/{phaseId}/bug-reports")
    public ResponseEntity<ApiResponse<List<BugReportResponse>>> listForPhase(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                ApiResponse.success(bugReportService.listForPhase(phaseId, CurrentUser.from(jwt))));
    }

    @GetMapping("/bug-reports/me")
    public ResponseEntity<ApiResponse<List<BugReportResponse>>> listMine(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(bugReportService.listMine(CurrentUser.from(jwt))));
    }

    @GetMapping("/bug-reports/{reportId}")
    public ResponseEntity<ApiResponse<BugReportDetailResponse>> get(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(bugReportService.get(reportId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/bug-reports/{reportId}/status")
    public ResponseEntity<ApiResponse<BugReportDetailResponse>> changeStatus(
            @PathVariable UUID reportId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StatusChangeRequest request) {
        BugReportDetailResponse updated = bugReportService.changeStatus(reportId, CurrentUser.from(jwt), request);
        return ResponseEntity.ok(ApiResponse.success("Bug report status updated", updated));
    }
}
