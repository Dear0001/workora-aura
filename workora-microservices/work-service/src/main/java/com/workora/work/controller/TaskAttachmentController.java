package com.workora.work.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.common.storage.StorageObjectService;
import com.workora.work.dto.TaskAttachmentRequest;
import com.workora.work.dto.TaskAttachmentResponse;
import com.workora.work.dto.TaskAttachmentUploadRequest;
import com.workora.work.security.CurrentUser;
import com.workora.work.service.TaskAttachmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}")
@SecurityRequirement(name = "bearerAuth")
public class TaskAttachmentController {

    private final TaskAttachmentService taskAttachmentService;

    public TaskAttachmentController(TaskAttachmentService taskAttachmentService) {
        this.taskAttachmentService = taskAttachmentService;
    }

    @GetMapping("/attachments")
    public ResponseEntity<ApiResponse<List<TaskAttachmentResponse>>> list(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                taskAttachmentService.list(taskId, CurrentUser.from(jwt))));
    }

    @PostMapping("/attachments/uploads")
    public ResponseEntity<ApiResponse<StorageObjectService.PresignedFileUpload>> createUpload(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskAttachmentUploadRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Upload URL created",
                taskAttachmentService.createUpload(taskId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/attachments")
    public ResponseEntity<ApiResponse<TaskAttachmentResponse>> add(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskAttachmentRequest request) {
        TaskAttachmentResponse response = taskAttachmentService.add(taskId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attachment saved", response));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<ApiResponse<Map<String, String>>> download(
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal Jwt jwt) {
        String downloadUrl = taskAttachmentService.downloadUrl(
                taskId, attachmentId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success(Map.of("downloadUrl", downloadUrl)));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID taskId,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal Jwt jwt) {
        taskAttachmentService.delete(taskId, attachmentId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted", null));
    }
}
