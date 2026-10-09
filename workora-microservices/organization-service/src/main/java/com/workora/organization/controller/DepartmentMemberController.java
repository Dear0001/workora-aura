package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.organization.dto.MemberResponse;
import com.workora.organization.dto.DepartmentRoleRequest;
import com.workora.organization.security.CurrentUser;
import com.workora.organization.service.MembershipService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments/{departmentId}/members")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentMemberController {
    private final MembershipService memberships;

    public DepartmentMemberController(MembershipService memberships) {
        this.memberships = memberships;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<MemberResponse>> add(@PathVariable UUID departmentId, @PathVariable UUID userId,
                                                            @AuthenticationPrincipal Jwt jwt,
                                                            @RequestBody(required = false) DepartmentRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Member assigned",
                memberships.assignToDepartment(departmentId, userId, CurrentUser.from(jwt).userId(),
                        request == null ? null : request.role())));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<MemberResponse>> changeRole(
            @PathVariable UUID departmentId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Department role updated",
                memberships.changeDepartmentRole(
                        departmentId, userId, CurrentUser.from(jwt).userId(), request.role())));
    }
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable UUID departmentId, @PathVariable UUID userId,
                                                     @AuthenticationPrincipal Jwt jwt) {
        memberships.removeFromDepartment(departmentId, userId, CurrentUser.from(jwt).userId());
        return ResponseEntity.ok(ApiResponse.success("Member removed from department", null));
    }
}
