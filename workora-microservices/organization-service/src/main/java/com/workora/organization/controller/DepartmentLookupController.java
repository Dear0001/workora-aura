package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.organization.dto.DepartmentResponse;
import com.workora.organization.dto.MemberResponse;
import com.workora.organization.security.CurrentUser;
import com.workora.organization.service.DepartmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Company-agnostic department lookup. Lets a member resolve a department (and its
 * owning company) by id without knowing the company up front. Other services use it
 * to map a department to its company for authorization decisions.
 */
@RestController
@RequestMapping("/api/v1/departments")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentLookupController {

    private final DepartmentService departmentService;

    public DepartmentLookupController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> get(
            @PathVariable UUID departmentId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                departmentService.getForCaller(departmentId, CurrentUser.from(jwt))));
    }

    @GetMapping("/{departmentId}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> members(
            @PathVariable UUID departmentId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                departmentService.listMembers(departmentId, CurrentUser.from(jwt))));
    }
}
