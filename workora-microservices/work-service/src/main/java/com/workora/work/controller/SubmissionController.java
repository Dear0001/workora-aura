package com.workora.work.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.work.dto.SubmissionRequest;
import com.workora.work.dto.SubmissionResponse;
import com.workora.work.security.CurrentUser;
import com.workora.work.service.SubmissionService;
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
@RequestMapping("/api/v1/tasks/{taskId}/submissions")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SubmissionRequest request) {
        SubmissionResponse submission = submissionService.submit(taskId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Submission recorded", submission));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> list(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.listForTask(taskId, CurrentUser.from(jwt))));
    }
}
