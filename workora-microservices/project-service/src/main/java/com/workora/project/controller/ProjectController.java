package com.workora.project.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.common.dto.PageResponse;
import com.workora.project.dto.ProjectRequest;
import com.workora.project.dto.ProjectResponse;
import com.workora.project.dto.ProjectUpdateRequest;
import com.workora.project.dto.PhaseMemberResponse;
import com.workora.project.security.CurrentUser;
import com.workora.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/departments/{departmentId}/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> createInDepartment(
            @PathVariable UUID departmentId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse created = projectService.createInDepartment(departmentId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Project created", created));
    }

    @GetMapping("/companies/{companyId}/projects")
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> listByCompany(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.listByCompany(companyId, CurrentUser.from(jwt), pageable)));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> get(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(projectService.get(projectId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Project updated",
                projectService.update(projectId, CurrentUser.from(jwt), request)));
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> archive(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Project archived",
                projectService.archive(projectId, CurrentUser.from(jwt))));
    }

    /**
     * Compatibility route for clients that expose archive as an explicit action.
     */
    @Operation(summary = "Archive a project")
    @PostMapping("/projects/{projectId}/archive")
    public ResponseEntity<ApiResponse<ProjectResponse>> archiveExplicitly(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Project archived",
                projectService.archive(projectId, CurrentUser.from(jwt))));
    }

    @GetMapping("/projects/{projectId}/members")
    public ResponseEntity<ApiResponse<List<PhaseMemberResponse>>> members(
            @PathVariable UUID projectId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                projectService.listMembers(projectId, CurrentUser.from(jwt))));
    }

    @PostMapping("/projects/{projectId}/managers/{userId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> manager(
            @PathVariable UUID projectId, @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Project manager updated",
                projectService.assignManager(projectId, userId, CurrentUser.from(jwt))));
    }
}
