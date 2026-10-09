package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.organization.dto.DepartmentRequest;
import com.workora.organization.dto.DepartmentResponse;
import com.workora.organization.security.CurrentUser;
import com.workora.organization.service.DepartmentService;
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
@RequestMapping("/api/v1/companies/{companyId}/departments")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse created = departmentService.create(companyId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Department created", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(departmentService.list(companyId, CurrentUser.from(jwt), search)));
    }

    @PatchMapping("/{departmentId}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable UUID companyId,
            @PathVariable UUID departmentId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Department updated",
                departmentService.update(companyId, departmentId, CurrentUser.from(jwt), request)));
    }

    @DeleteMapping("/{departmentId}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> archive(
            @PathVariable UUID companyId,
            @PathVariable UUID departmentId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Department archived",
                departmentService.archive(companyId, departmentId, CurrentUser.from(jwt))));
    }
}
