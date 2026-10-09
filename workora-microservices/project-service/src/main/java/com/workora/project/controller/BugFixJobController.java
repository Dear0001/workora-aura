package com.workora.project.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.project.dto.ApproveFeeRequest;
import com.workora.project.dto.BugFixJobRequest;
import com.workora.project.dto.BugFixJobResponse;
import com.workora.project.dto.BugFixJobUpdateRequest;
import com.workora.project.dto.ConfirmReceiptRequest;
import com.workora.project.dto.JobActionRequest;
import com.workora.project.dto.JobApplicationRequest;
import com.workora.project.dto.JobApplicationResponse;
import com.workora.project.dto.JobReviewRequest;
import com.workora.project.dto.JobReviewResponse;
import com.workora.project.dto.JobSubmissionRequest;
import com.workora.project.dto.JobSubmissionResponse;
import com.workora.project.dto.RecordPaymentRequest;
import com.workora.project.dto.SelectFreelancerRequest;
import com.workora.project.security.CurrentUser;
import com.workora.project.service.BugFixJobService;
import com.workora.project.service.JobApplicationService;
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
public class BugFixJobController {

    private final BugFixJobService jobService;
    private final JobApplicationService applicationService;

    public BugFixJobController(BugFixJobService jobService, JobApplicationService applicationService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
    }

    @PostMapping("/phases/{phaseId}/bug-fix-jobs")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> create(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BugFixJobRequest request) {
        BugFixJobResponse created = jobService.create(phaseId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Bug-Fix Job created", created));
    }

    @GetMapping("/phases/{phaseId}/bug-fix-jobs")
    public ResponseEntity<ApiResponse<List<BugFixJobResponse>>> listForPhase(
            @PathVariable UUID phaseId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(jobService.listForPhase(phaseId, CurrentUser.from(jwt))));
    }

    @GetMapping("/companies/{companyId}/bug-fix-jobs")
    public ResponseEntity<ApiResponse<List<BugFixJobResponse>>> listForCompany(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(jobService.listForCompany(companyId, CurrentUser.from(jwt))));
    }

    @GetMapping("/bug-fix-jobs/me")
    public ResponseEntity<ApiResponse<List<BugFixJobResponse>>> listMine(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(jobService.listMine(CurrentUser.from(jwt))));
    }

    @GetMapping("/bug-fix-jobs/{jobId}")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> get(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(jobService.get(jobId, CurrentUser.from(jwt))));
    }

    @PatchMapping("/bug-fix-jobs/{jobId}")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> update(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BugFixJobUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bug-Fix Job updated",
                jobService.update(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/publish")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> publish(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) JobActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bug-Fix Job published",
                jobService.publish(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/select-freelancer")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> selectFreelancer(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SelectFreelancerRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Freelancer selected",
                jobService.selectFreelancer(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/submissions")
    public ResponseEntity<ApiResponse<JobSubmissionResponse>> submit(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobSubmissionRequest request) {
        JobSubmissionResponse submission = jobService.submit(jobId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Submission recorded", submission));
    }

    @GetMapping("/bug-fix-jobs/{jobId}/submissions")
    public ResponseEntity<ApiResponse<List<JobSubmissionResponse>>> listSubmissions(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(jobService.listSubmissions(jobId, CurrentUser.from(jwt))));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/reviews")
    public ResponseEntity<ApiResponse<JobReviewResponse>> recordReview(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobReviewRequest request) {
        JobReviewResponse review = jobService.recordReview(jobId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Review recorded", review));
    }

    @GetMapping("/bug-fix-jobs/{jobId}/reviews")
    public ResponseEntity<ApiResponse<List<JobReviewResponse>>> listReviews(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(jobService.listReviews(jobId, CurrentUser.from(jwt))));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/approve-fee")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> approveFee(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApproveFeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Fee approved",
                jobService.approveFee(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/payment")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> recordPayment(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment recorded",
                jobService.recordPayment(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/confirm-receipt")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> confirmReceipt(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) ConfirmReceiptRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Receipt confirmed",
                jobService.confirmReceipt(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/close")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> close(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) JobActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bug-Fix Job closed",
                jobService.close(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/cancel")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> cancel(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) JobActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bug-Fix Job cancelled",
                jobService.cancel(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/dispute")
    public ResponseEntity<ApiResponse<BugFixJobResponse>> dispute(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) JobActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dispute opened",
                jobService.openDispute(jobId, CurrentUser.from(jwt), request)));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<JobApplicationResponse>> apply(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobApplicationRequest request) {
        JobApplicationResponse application = applicationService.apply(jobId, CurrentUser.from(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Application submitted", application));
    }

    @GetMapping("/bug-fix-jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<List<JobApplicationResponse>>> listApplications(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.listForJob(jobId, CurrentUser.from(jwt))));
    }

    @PostMapping("/bug-fix-jobs/{jobId}/applications/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal Jwt jwt) {
        applicationService.withdraw(jobId, CurrentUser.from(jwt));
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn", null));
    }
}
