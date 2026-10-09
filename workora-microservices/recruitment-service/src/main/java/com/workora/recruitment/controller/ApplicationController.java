package com.workora.recruitment.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.recruitment.dto.ApplicationFileRequest;
import com.workora.recruitment.dto.ApplicationFileResponse;
import com.workora.recruitment.dto.ApplicationRequest;
import com.workora.recruitment.dto.ApplicationResponse;
import com.workora.recruitment.dto.ReviewerApplicationResponse;
import com.workora.recruitment.dto.StatusChangeRequest;
import com.workora.recruitment.security.CurrentUser;
import com.workora.recruitment.service.ApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/recruitment-posts/{postId}/applications")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse created = applicationService.apply(postId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted", created));
    }

    @GetMapping("/recruitment-posts/{postId}/applications")
    public ResponseEntity<ApiResponse<List<ReviewerApplicationResponse>>> listForPost(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listForPost(postId, CurrentUser.from(jwt))));
    }

    @GetMapping("/applications/me")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> listMine(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listMine(CurrentUser.from(jwt))));
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApiResponse<Object>> get(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.get(applicationId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/applications/{applicationId}/status")
    public ResponseEntity<ApiResponse<Object>> changeStatus(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application status updated",
                applicationService.changeStatus(applicationId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/applications/{applicationId}/files")
    public ResponseEntity<ApiResponse<ApplicationFileResponse>> addFile(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApplicationFileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("File attached",
                applicationService.addFile(applicationId, CurrentUser.from(jwt), request)));
    }

    @GetMapping("/applications/{applicationId}/files")
    public ResponseEntity<ApiResponse<List<ApplicationFileResponse>>> listFiles(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.listFiles(applicationId, CurrentUser.from(jwt))));
    }
}
