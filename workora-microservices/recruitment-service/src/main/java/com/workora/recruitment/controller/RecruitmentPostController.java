package com.workora.recruitment.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.recruitment.dto.PublicPostResponse;
import com.workora.recruitment.dto.RecruitmentPostRequest;
import com.workora.recruitment.dto.RecruitmentPostResponse;
import com.workora.recruitment.dto.RecruitmentPostUpdateRequest;
import com.workora.recruitment.security.CurrentUser;
import com.workora.recruitment.service.RecruitmentPostService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "bearerAuth")
public class RecruitmentPostController {

    private final RecruitmentPostService postService;

    public RecruitmentPostController(RecruitmentPostService postService) {
        this.postService = postService;
    }

    @PostMapping("/recruitment-posts")
    public ResponseEntity<ApiResponse<RecruitmentPostResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecruitmentPostRequest request) {
        RecruitmentPostResponse created = postService.create(CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recruitment post created", created));
    }

    @PostMapping("/recruitment-posts/{postId}/publish")
    public ResponseEntity<ApiResponse<RecruitmentPostResponse>> publish(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Recruitment post published",
                postService.publish(postId, CurrentUser.from(jwt))));
    }

    @GetMapping("/recruitment-posts/{postId}")
    public ResponseEntity<ApiResponse<RecruitmentPostResponse>> get(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(postService.get(postId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/recruitment-posts/{postId}")
    public ResponseEntity<ApiResponse<RecruitmentPostResponse>> update(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecruitmentPostUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Recruitment post updated",
                postService.update(postId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/recruitment-posts/{postId}/close")
    public ResponseEntity<ApiResponse<RecruitmentPostResponse>> close(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Recruitment post closed",
                postService.close(postId, CurrentUser.from(jwt))));
    }

    @DeleteMapping("/recruitment-posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID postId,
            @AuthenticationPrincipal Jwt jwt) {
        postService.delete(postId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success("Recruitment post deleted", null));
    }

    @GetMapping("/companies/{companyId}/recruitment-posts")
    public ResponseEntity<ApiResponse<List<PublicPostResponse>>> listByCompany(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.listByCompany(companyId, CurrentUser.from(jwt))));
    }

    @GetMapping("/public/recruitment-posts")
    public ResponseEntity<ApiResponse<List<PublicPostResponse>>> listPublic(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(
                postService.listPublic(CurrentUser.from(jwt))));
    }
}
