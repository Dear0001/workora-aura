package com.workora.project.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.common.dto.PageResponse;
import com.workora.project.dto.PublicPhaseResponse;
import com.workora.project.service.PhaseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public discovery of published public phases. Any authenticated user may browse;
 * the response is a public-safe projection with no private fields.
 */
@RestController
@RequestMapping("/api/v1/public/phases")
@SecurityRequirement(name = "bearerAuth")
public class PublicPhaseController {

    private final PhaseService phaseService;

    public PublicPhaseController(PhaseService phaseService) {
        this.phaseService = phaseService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PublicPhaseResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(phaseService.listPublic(pageable)));
    }
}
