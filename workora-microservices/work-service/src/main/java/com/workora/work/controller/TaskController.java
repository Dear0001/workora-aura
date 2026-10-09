package com.workora.work.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.work.dto.TaskDetailResponse;
import com.workora.work.dto.TaskHistoryResponse;
import com.workora.work.dto.TaskRequest;
import com.workora.work.dto.TaskResponse;
import com.workora.work.dto.TaskStatusChangeRequest;
import com.workora.work.dto.TaskSummaryResponse;
import com.workora.work.dto.TaskUpdateRequest;
import com.workora.work.security.CurrentUser;
import com.workora.work.service.TaskService;
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
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/phases/{phaseId}/tasks")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.create(phaseId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Task created", created));
    }

    @GetMapping("/phases/{phaseId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listForPhase(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(taskService.listForPhase(phaseId, CurrentUser.from(jwt))));
    }

    @GetMapping("/tasks/me")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> listMyAssigned(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(taskService.listMyAssigned(CurrentUser.from(jwt))));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskDetailResponse>> get(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(taskService.get(taskId, CurrentUser.from(jwt))));
    }

    @GetMapping("/tasks/{taskId}/history")
    public ResponseEntity<ApiResponse<List<TaskHistoryResponse>>> history(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(taskService.history(taskId, CurrentUser.from(jwt))));
    }

    @GetMapping("/tasks/{taskId}/summary")
    public ResponseEntity<ApiResponse<TaskSummaryResponse>> summary(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(taskService.summary(taskId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task updated",
                taskService.update(taskId, CurrentUser.from(jwt), request)));
    }

    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> changeStatus(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody TaskStatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Task status updated",
                taskService.changeStatus(taskId, CurrentUser.from(jwt), request.status(), request.reason())));
    }
}
