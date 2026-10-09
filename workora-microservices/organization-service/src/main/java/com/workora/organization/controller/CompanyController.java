package com.workora.organization.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.common.dto.PageResponse;
import com.workora.organization.dto.CompanyRatingRequest;
import com.workora.organization.dto.CompanyRatingResponse;
import com.workora.organization.dto.CompanyRequest;
import com.workora.organization.dto.CompanyResponse;
import com.workora.organization.dto.CompanyUpdateRequest;
import com.workora.organization.security.CurrentUser;
import com.workora.organization.service.CompanyRatingService;
import com.workora.organization.service.CompanyService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@SecurityRequirement(name = "bearerAuth")
public class CompanyController {

    private final CompanyService companyService;
    private final CompanyRatingService companyRatingService;

    public CompanyController(CompanyService companyService, CompanyRatingService companyRatingService) {
        this.companyService = companyService;
        this.companyRatingService = companyRatingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CompanyResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CompanyRequest request) {
        CompanyResponse created = companyService.create(CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Company created", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CompanyResponse>>> listMine(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(companyService.listMine(CurrentUser.from(jwt), search, pageable)));
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID companyId) {
        return ResponseEntity.ok(ApiResponse.success(companyService.get(companyId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Company updated",
                companyService.update(companyId, CurrentUser.from(jwt), request)));
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<ApiResponse<CompanyResponse>> archive(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID companyId) {
        return ResponseEntity.ok(ApiResponse.success("Company archived",
                companyService.archive(companyId, CurrentUser.from(jwt))));
    }

    @PostMapping("/{companyId}/ratings")
    public ResponseEntity<ApiResponse<CompanyRatingResponse>> rate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyRatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Company rating saved",
                companyRatingService.rateCompany(companyId, CurrentUser.from(jwt), request)));
    }

    @GetMapping("/{companyId}/ratings")
    public ResponseEntity<ApiResponse<Page<CompanyRatingResponse>>> ratings(
            @PathVariable UUID companyId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(companyRatingService.list(companyId, pageable)));
    }
}
