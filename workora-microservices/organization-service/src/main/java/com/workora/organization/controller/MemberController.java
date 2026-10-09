package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.organization.dto.ChangeRoleRequest;
import com.workora.organization.dto.MemberResponse;
import com.workora.organization.entity.MembershipAudit;
import com.workora.organization.security.CurrentUser;
import com.workora.organization.service.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/members")
@SecurityRequirement(name = "bearerAuth")
public class MemberController {

    private final MembershipService membershipService;

    public MemberController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MemberResponse>>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.listMembers(companyId, CurrentUser.from(jwt).userId(), search)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> myMembership(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.getMyMembership(companyId, CurrentUser.from(jwt).userId())));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.getMember(companyId, userId, CurrentUser.from(jwt).userId())));
    }

    @GetMapping("/{userId}/audit")
    public ResponseEntity<ApiResponse<List<MembershipAudit>>> audit(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.auditFor(companyId, userId, CurrentUser.from(jwt).userId())));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemberResponse>> changeRole(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Member role updated",
                membershipService.changeRole(companyId, userId, CurrentUser.from(jwt).userId(), request)));
    }

    @Operation(summary = "Assign a role to a company member")
    @PostMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<MemberResponse>> assignRole(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeRoleRequest request) {
        return changeRole(companyId, userId, jwt, request);
    }

    @Operation(summary = "Update a company member role")
    @PatchMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<MemberResponse>> updateRole(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeRoleRequest request) {
        return changeRole(companyId, userId, jwt, request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        membershipService.removeMember(companyId, userId, CurrentUser.from(jwt).userId());
        return ResponseEntity.ok(ApiResponse.success("Member removed", null));
    }
}
