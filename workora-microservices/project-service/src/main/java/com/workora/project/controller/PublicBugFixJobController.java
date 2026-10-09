package com.workora.project.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.common.dto.PageResponse;
import com.workora.project.dto.PublicBugFixJobResponse;
import com.workora.project.service.BugFixJobService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public discovery of OPEN bug-fix jobs for the freelancer newsfeed. Any authenticated
 * user may browse; the response is a public-safe projection with no private fields.
 */
@RestController
@RequestMapping("/api/v1/public/bug-fix-jobs")
@SecurityRequirement(name = "bearerAuth")
public class PublicBugFixJobController {

    private final BugFixJobService jobService;

    public PublicBugFixJobController(BugFixJobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PublicBugFixJobResponse>>> list(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(jobService.listPublic(pageable)));
    }
}
