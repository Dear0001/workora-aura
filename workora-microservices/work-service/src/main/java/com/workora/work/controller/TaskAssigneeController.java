package com.workora.work.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.work.dto.AssigneeRequest;
import com.workora.work.dto.AssigneeResponse;
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
@RequestMapping("/api/v1/tasks/{taskId}/assignees")
@SecurityRequirement(name = "bearerAuth")
public class TaskAssigneeController {

    private final TaskService taskService;

    public TaskAssigneeController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssigneeResponse>> add(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AssigneeRequest request) {
        AssigneeResponse assignee = taskService.addAssignee(taskId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Assignee added", assignee));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssigneeResponse>>> list(
            @PathVariable UUID taskId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(taskService.listAssignees(taskId, CurrentUser.from(jwt))));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable UUID taskId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        taskService.removeAssignee(taskId, userId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success("Assignee removed", null));
    }
}
