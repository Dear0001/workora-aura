package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.organization.dto.AcceptInvitationRequest;
import com.workora.organization.dto.InvitationRequest;
import com.workora.organization.dto.InvitationResponse;
import com.workora.organization.security.CurrentUser;
import com.workora.organization.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/companies/{companyId}/invitations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InvitationRequest request) {
        InvitationService.CreatedInvitation created = invitationService.create(companyId, CurrentUser.from(jwt), request);
        Map<String, Object> body = Map.of(
                "invitation", created.invitation(),
                "acceptToken", created.token()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Invitation created", body));
    }

    @GetMapping("/companies/{companyId}/invitations")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(invitationService.list(companyId, CurrentUser.from(jwt))));
    }

    @GetMapping("/invitations/pending")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> listPending(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                invitationService.listPending(CurrentUser.from(jwt))));
    }

    @DeleteMapping("/companies/{companyId}/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<InvitationResponse>> revoke(
            @PathVariable UUID companyId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Invitation revoked",
                invitationService.revoke(companyId, invitationId, CurrentUser.from(jwt))));
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<ApiResponse<InvitationResponse>> accept(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AcceptInvitationRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted",
                invitationService.accept(CurrentUser.from(jwt), request.token())));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<InvitationResponse>> acceptById(
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted",
                invitationService.accept(CurrentUser.from(jwt), invitationId)));
    }

    @Operation(summary = "Accept a company invitation by ID")
    @PostMapping("/companies/{companyId}/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<InvitationResponse>> acceptByCompanyPath(
            @PathVariable UUID companyId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal Jwt jwt) {
        return acceptById(invitationId, jwt);
    }
}
