package com.workora.project.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.project.dto.PhaseMemberRequest;
import com.workora.project.dto.PhaseMemberResponse;
import com.workora.project.dto.PhaseRequest;
import com.workora.project.dto.PhaseResponse;
import com.workora.project.dto.PhaseUpdateRequest;
import com.workora.project.entity.PhaseStatus;
import com.workora.project.security.CurrentUser;
import com.workora.project.service.PhaseMemberService;
import com.workora.project.service.PhaseService;
import io.swagger.v3.oas.annotations.Operation;
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
public class PhaseController {

    private final PhaseService phaseService;
    private final PhaseMemberService phaseMemberService;

    public PhaseController(PhaseService phaseService, PhaseMemberService phaseMemberService) {
        this.phaseService = phaseService;
        this.phaseMemberService = phaseMemberService;
    }

    @PostMapping("/projects/{projectId}/phases")
    public ResponseEntity<ApiResponse<PhaseResponse>> create(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhaseRequest request) {
        PhaseResponse created = phaseService.create(projectId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Phase created", created));
    }

    @GetMapping("/projects/{projectId}/phases")
    public ResponseEntity<ApiResponse<List<PhaseResponse>>> listForProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(phaseService.listForProject(projectId, CurrentUser.from(jwt))));
    }

    @GetMapping("/phases/{phaseId}")
    public ResponseEntity<ApiResponse<PhaseResponse>> get(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(phaseService.get(phaseId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/phases/{phaseId}")
    public ResponseEntity<ApiResponse<PhaseResponse>> update(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhaseUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Phase updated",
                phaseService.update(phaseId, CurrentUser.from(jwt), request)));
    }

    @Operation(summary = "Archive a phase")
    @DeleteMapping("/phases/{phaseId}")
    public ResponseEntity<ApiResponse<PhaseResponse>> archive(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Phase archived",
                phaseService.update(phaseId, CurrentUser.from(jwt),
                        new PhaseUpdateRequest(null, null, null, PhaseStatus.ARCHIVED))));
    }

    @Operation(summary = "Publish a phase")
    @PostMapping("/phases/{phaseId}/publish")
    public ResponseEntity<ApiResponse<PhaseResponse>> publish(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return updateStatus(phaseId, jwt, PhaseStatus.PUBLISHED, "Phase published");
    }

    @Operation(summary = "Hide a phase")
    @PostMapping("/phases/{phaseId}/hide")
    public ResponseEntity<ApiResponse<PhaseResponse>> hide(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return updateStatus(phaseId, jwt, PhaseStatus.ARCHIVED, "Phase hidden");
    }

    @Operation(summary = "Close a phase")
    @PostMapping("/phases/{phaseId}/close")
    public ResponseEntity<ApiResponse<PhaseResponse>> close(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return updateStatus(phaseId, jwt, PhaseStatus.CLOSED, "Phase closed");
    }

    private ResponseEntity<ApiResponse<PhaseResponse>> updateStatus(
            UUID phaseId, Jwt jwt, PhaseStatus status, String message) {
        return ResponseEntity.ok(ApiResponse.success(message,
                phaseService.update(phaseId, CurrentUser.from(jwt),
                        new PhaseUpdateRequest(null, null, null, status))));
    }

    @PostMapping("/phases/{phaseId}/members")
    public ResponseEntity<ApiResponse<PhaseMemberResponse>> addMember(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PhaseMemberRequest request) {
        PhaseMemberResponse member = phaseMemberService.add(phaseId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Phase member added", member));
    }

    @Operation(summary = "Add a phase member by user ID")
    @PostMapping("/phases/{phaseId}/members/{userId}")
    public ResponseEntity<ApiResponse<PhaseMemberResponse>> addMemberByPath(
            @PathVariable UUID phaseId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        PhaseMemberResponse member = phaseMemberService.add(
                phaseId, CurrentUser.from(jwt), new PhaseMemberRequest(userId, null));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Phase member added", member));
    }

    @GetMapping("/phases/{phaseId}/members")
    public ResponseEntity<ApiResponse<List<PhaseMemberResponse>>> listMembers(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(phaseMemberService.list(phaseId, CurrentUser.from(jwt))));
    }

    @DeleteMapping("/phases/{phaseId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID phaseId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        phaseMemberService.remove(phaseId, userId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success("Phase member removed", null));
    }
}
