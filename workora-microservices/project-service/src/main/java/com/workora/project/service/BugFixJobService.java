package com.workora.project.service;

import com.workora.common.dto.PageResponse;
import com.workora.project.dto.ApproveFeeRequest;
import com.workora.project.dto.BugFixJobRequest;
import com.workora.project.dto.BugFixJobResponse;
import com.workora.project.dto.BugFixJobUpdateRequest;
import com.workora.project.dto.ConfirmReceiptRequest;
import com.workora.project.dto.JobActionRequest;
import com.workora.project.dto.JobReviewRequest;
import com.workora.project.dto.JobReviewResponse;
import com.workora.project.dto.JobSubmissionRequest;
import com.workora.project.dto.JobSubmissionResponse;
import com.workora.project.dto.PaymentStage;
import com.workora.project.dto.PublicBugFixJobResponse;
import com.workora.project.dto.RecordPaymentRequest;
import com.workora.project.dto.SelectFreelancerRequest;
import com.workora.project.entity.BugFixJob;
import com.workora.project.entity.BugFixJobApplication;
import com.workora.project.entity.BugFixJobReview;
import com.workora.project.entity.BugFixJobStatus;
import com.workora.project.entity.BugFixJobStatusHistory;
import com.workora.project.entity.BugFixJobSubmission;
import com.workora.project.entity.JobApplicationDecision;
import com.workora.project.entity.Phase;
import com.workora.project.entity.Project;
import com.workora.project.event.ProjectEventPublisher;
import com.workora.project.exception.AppException;
import com.workora.project.exception.ConflictException;
import com.workora.project.exception.NotFoundException;
import com.workora.project.mapper.ProjectMapper;
import com.workora.project.repository.BugFixJobApplicationRepository;
import com.workora.project.repository.BugFixJobRepository;
import com.workora.project.repository.BugFixJobReviewRepository;
import com.workora.project.repository.BugFixJobStatusHistoryRepository;
import com.workora.project.repository.BugFixJobSubmissionRepository;
import com.workora.project.repository.PhaseRepository;
import com.workora.project.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class BugFixJobService {

    /** Allowed status transitions. DISPUTED is reachable from any non-terminal, pre-receipt state. */
    private static final Map<BugFixJobStatus, Set<BugFixJobStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(BugFixJobStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.DRAFT, EnumSet.of(
                BugFixJobStatus.OPEN, BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.OPEN, EnumSet.of(
                BugFixJobStatus.ASSIGNED, BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.ASSIGNED, EnumSet.of(
                BugFixJobStatus.SUBMITTED, BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.SUBMITTED, EnumSet.of(
                BugFixJobStatus.UNDER_REVIEW, BugFixJobStatus.CHANGES_REQUESTED,
                BugFixJobStatus.TEST_FAILED, BugFixJobStatus.TEST_PASSED,
                BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.UNDER_REVIEW, EnumSet.of(
                BugFixJobStatus.CHANGES_REQUESTED, BugFixJobStatus.TEST_FAILED,
                BugFixJobStatus.TEST_PASSED, BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.CHANGES_REQUESTED, EnumSet.of(
                BugFixJobStatus.SUBMITTED, BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.TEST_FAILED, EnumSet.of(
                BugFixJobStatus.SUBMITTED, BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.TEST_PASSED, EnumSet.of(
                BugFixJobStatus.FEE_APPROVED, BugFixJobStatus.CANCELLED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.FEE_APPROVED, EnumSet.of(
                BugFixJobStatus.WAITING_FOR_COMPANY_PAYMENT, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.WAITING_FOR_COMPANY_PAYMENT, EnumSet.of(
                BugFixJobStatus.PAYMENT_VERIFIED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.PAYMENT_VERIFIED, EnumSet.of(
                BugFixJobStatus.FEE_RECEIPT_CONFIRMED, BugFixJobStatus.CLOSED, BugFixJobStatus.DISPUTED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.FEE_RECEIPT_CONFIRMED, EnumSet.of(
                BugFixJobStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.DISPUTED, EnumSet.of(
                BugFixJobStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.CLOSED, EnumSet.noneOf(BugFixJobStatus.class));
        ALLOWED_TRANSITIONS.put(BugFixJobStatus.CANCELLED, EnumSet.noneOf(BugFixJobStatus.class));
    }

    private final BugFixJobRepository jobRepository;
    private final PhaseRepository phaseRepository;
    private final BugFixJobApplicationRepository applicationRepository;
    private final BugFixJobSubmissionRepository submissionRepository;
    private final BugFixJobReviewRepository reviewRepository;
    private final BugFixJobStatusHistoryRepository historyRepository;
    private final ProjectService projectService;
    private final ProjectAuthorization projectAuthorization;
    private final BugFixJobAuthorization jobAuthorization;
    private final ProjectMapper mapper;
    private final ProjectEventPublisher projectEventPublisher;

    public BugFixJobService(BugFixJobRepository jobRepository,
                            PhaseRepository phaseRepository,
                            BugFixJobApplicationRepository applicationRepository,
                            BugFixJobSubmissionRepository submissionRepository,
                            BugFixJobReviewRepository reviewRepository,
                            BugFixJobStatusHistoryRepository historyRepository,
                            ProjectService projectService,
                            ProjectAuthorization projectAuthorization,
                            BugFixJobAuthorization jobAuthorization,
                            ProjectMapper mapper,
                            ProjectEventPublisher projectEventPublisher) {
        this.jobRepository = jobRepository;
        this.phaseRepository = phaseRepository;
        this.applicationRepository = applicationRepository;
        this.submissionRepository = submissionRepository;
        this.reviewRepository = reviewRepository;
        this.historyRepository = historyRepository;
        this.projectService = projectService;
        this.projectAuthorization = projectAuthorization;
        this.jobAuthorization = jobAuthorization;
        this.mapper = mapper;
        this.projectEventPublisher = projectEventPublisher;
    }

    @Transactional
    public BugFixJobResponse create(UUID phaseId, CurrentUser caller, BugFixJobRequest request) {
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new NotFoundException("Phase not found"));
        Project project = projectService.loadActive(phase.getProjectId());
        projectAuthorization.requireCanManageProject(project, caller);

        BugFixJob job = new BugFixJob(phaseId, project.getId(), project.getDepartmentId(),
                project.getCompanyId(), project.getProjectManagerId(), request.title().trim());
        job.setTaskId(request.taskId());
        job.setSummary(request.summary());
        job.setProductType(request.productType());
        job.setTechStack(request.techStack());
        job.setVersionBuild(request.versionBuild());
        job.setEnvironments(request.environments());
        job.setScope(request.scope());
        job.setOutOfScope(request.outOfScope());
        job.setAuthorizedTestingRules(request.authorizedTestingRules());
        job.setAccessMethod(request.accessMethod());
        job.setCategory(request.category());
        job.setSeverityCriteria(request.severityCriteria());
        job.setReproductionRequirements(request.reproductionRequirements());
        job.setEvidenceRequirements(request.evidenceRequirements());
        job.setFixExpectation(request.fixExpectation());
        if (request.requiredSkills() != null) {
            job.setRequiredSkills(request.requiredSkills());
        }
        job.setEligibilityNotes(request.eligibilityNotes());
        if (request.requiresNda() != null) {
            job.setRequiresNda(request.requiresNda());
        }
        job.setFee(request.fee());
        job.setCurrency(request.currency());
        if (request.splitFeeAllowed() != null) {
            job.setSplitFeeAllowed(request.splitFeeAllowed());
        }
        job.setDuplicateReportRule(request.duplicateReportRule());
        job.setMaxAcceptedReports(request.maxAcceptedReports());
        if (request.closesAfterOneResolution() != null) {
            job.setClosesAfterOneResolution(request.closesAfterOneResolution());
        }
        job.setDeadline(request.deadline());
        job.setSafeHarborStatement(request.safeHarborStatement());
        job.setStatus(BugFixJobStatus.DRAFT);
        job.setJobVersion(1);

        BugFixJob saved = jobRepository.save(job);
        historyRepository.save(new BugFixJobStatusHistory(
                saved.getId(), null, BugFixJobStatus.DRAFT, caller.userId(), "Job created"));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", saved.getId());
        if (saved.getPhaseId() != null) {
            payload.put("phaseId", saved.getPhaseId());
        }
        if (saved.getStatus() != null) {
            payload.put("status", saved.getStatus().name());
        }
        projectEventPublisher.publish(
                "job.created",
                saved.getProjectId(),
                saved.getCompanyId(),
                caller.userId(),
                saved.getProjectManagerId(),
                payload
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public BugFixJobResponse publish(UUID jobId, CurrentUser caller, JobActionRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanManageJob(job, caller);
        transition(job, BugFixJobStatus.OPEN, caller.userId(), reason(request));
        return mapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public BugFixJobResponse get(UUID jobId, CurrentUser caller) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanViewJob(job, caller);
        return mapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<BugFixJobResponse> listForPhase(UUID phaseId, CurrentUser caller) {
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new NotFoundException("Phase not found"));
        Project project = projectService.loadActive(phase.getProjectId());
        projectAuthorization.requireCompanyMember(project.getCompanyId());
        return jobRepository.findByPhaseIdOrderByCreatedAtDesc(phaseId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BugFixJobResponse> listForCompany(UUID companyId, CurrentUser caller) {
        projectAuthorization.requireCompanyMember(companyId);
        return jobRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BugFixJobResponse> listMine(CurrentUser caller) {
        Map<UUID, BugFixJob> unique = new LinkedHashMap<>();
        for (BugFixJob job : jobRepository.findBySelectedFreelancerId(caller.userId())) {
            unique.putIfAbsent(job.getId(), job);
        }
        for (BugFixJob job : jobRepository.findByProjectManagerId(caller.userId())) {
            unique.putIfAbsent(job.getId(), job);
        }
        for (BugFixJobApplication application : applicationRepository.findByApplicantId(caller.userId())) {
            UUID jobId = application.getJobId();
            if (!unique.containsKey(jobId)) {
                jobRepository.findById(jobId).ifPresent(job -> unique.putIfAbsent(jobId, job));
            }
        }
        return unique.values().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicBugFixJobResponse> listPublic(Pageable pageable) {
        Page<BugFixJob> page = jobRepository.findByStatus(BugFixJobStatus.OPEN, pageable);
        List<PublicBugFixJobResponse> content = page.getContent().stream()
                .map(mapper::toPublicResponse)
                .toList();
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional
    public BugFixJobResponse update(UUID jobId, CurrentUser caller, BugFixJobUpdateRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanManageJob(job, caller);
        boolean assigned = job.getSelectedFreelancerId() != null;

        // Locked-field rule: the fixed fee, currency, acceptance criteria and category are the
        // basis of the agreement and cannot move once a freelancer has been assigned.
        if (assigned) {
            boolean feeChanged = request.fee() != null && job.getFee() != null
                    && request.fee().compareTo(job.getFee()) != 0;
            boolean currencyChanged = request.currency() != null
                    && !request.currency().equalsIgnoreCase(job.getCurrency());
            boolean expectationChanged = request.fixExpectation() != null
                    && request.fixExpectation() != job.getFixExpectation();
            boolean categoryChanged = request.category() != null
                    && request.category() != job.getCategory();
            if (feeChanged || currencyChanged || expectationChanged || categoryChanged) {
                throw new ConflictException(
                        "Fee, currency, acceptance criteria and category cannot change after a freelancer is assigned");
            }
        }

        if (request.title() != null && !request.title().isBlank()) {
            job.setTitle(request.title().trim());
        }
        if (request.summary() != null) {
            job.setSummary(request.summary());
        }
        if (request.productType() != null) {
            job.setProductType(request.productType());
        }
        if (request.techStack() != null) {
            job.setTechStack(request.techStack());
        }
        if (request.versionBuild() != null) {
            job.setVersionBuild(request.versionBuild());
        }
        if (request.environments() != null) {
            job.setEnvironments(request.environments());
        }
        if (request.scope() != null) {
            job.setScope(request.scope());
        }
        if (request.outOfScope() != null) {
            job.setOutOfScope(request.outOfScope());
        }
        if (request.authorizedTestingRules() != null) {
            job.setAuthorizedTestingRules(request.authorizedTestingRules());
        }
        if (request.accessMethod() != null) {
            job.setAccessMethod(request.accessMethod());
        }
        if (request.category() != null) {
            job.setCategory(request.category());
        }
        if (request.severityCriteria() != null) {
            job.setSeverityCriteria(request.severityCriteria());
        }
        if (request.reproductionRequirements() != null) {
            job.setReproductionRequirements(request.reproductionRequirements());
        }
        if (request.evidenceRequirements() != null) {
            job.setEvidenceRequirements(request.evidenceRequirements());
        }
        if (request.fixExpectation() != null) {
            job.setFixExpectation(request.fixExpectation());
        }
        if (request.requiredSkills() != null) {
            job.setRequiredSkills(request.requiredSkills());
        }
        if (request.eligibilityNotes() != null) {
            job.setEligibilityNotes(request.eligibilityNotes());
        }
        if (request.requiresNda() != null) {
            job.setRequiresNda(request.requiresNda());
        }
        if (request.fee() != null) {
            job.setFee(request.fee());
        }
        if (request.currency() != null) {
            job.setCurrency(request.currency());
        }
        if (request.splitFeeAllowed() != null) {
            job.setSplitFeeAllowed(request.splitFeeAllowed());
        }
        if (request.duplicateReportRule() != null) {
            job.setDuplicateReportRule(request.duplicateReportRule());
        }
        if (request.maxAcceptedReports() != null) {
            job.setMaxAcceptedReports(request.maxAcceptedReports());
        }
        if (request.closesAfterOneResolution() != null) {
            job.setClosesAfterOneResolution(request.closesAfterOneResolution());
        }
        if (request.deadline() != null) {
            job.setDeadline(request.deadline());
        }
        if (request.safeHarborStatement() != null) {
            job.setSafeHarborStatement(request.safeHarborStatement());
        }
        if (request.taskId() != null) {
            job.setTaskId(request.taskId());
        }

        if (assigned) {
            job.setJobVersion(job.getJobVersion() + 1);
        }
        return mapper.toResponse(jobRepository.save(job));
    }

    @Transactional
    public BugFixJobResponse selectFreelancer(UUID jobId, CurrentUser caller, SelectFreelancerRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanManageJob(job, caller);
        if (job.getStatus() != BugFixJobStatus.OPEN) {
            throw new AppException("A freelancer can only be selected while the job is open");
        }
        BugFixJobApplication application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (!application.getJobId().equals(job.getId())) {
            throw new NotFoundException("Application not found");
        }
        if (application.getDecision() != JobApplicationDecision.PENDING) {
            throw new AppException("Only a pending application can be selected");
        }

        Instant now = Instant.now();
        job.setSelectedFreelancerId(application.getApplicantId());
        application.setDecision(JobApplicationDecision.SELECTED);
        application.setDecidedAt(now);
        applicationRepository.save(application);

        for (BugFixJobApplication other : applicationRepository.findByJobIdOrderByCreatedAtAsc(job.getId())) {
            if (!other.getId().equals(application.getId())
                    && other.getDecision() == JobApplicationDecision.PENDING) {
                other.setDecision(JobApplicationDecision.REJECTED);
                other.setDecidedAt(now);
                applicationRepository.save(other);
            }
        }

        transition(job, BugFixJobStatus.ASSIGNED, caller.userId(), request.reason());
        return mapper.toResponse(job);
    }

    @Transactional
    public JobSubmissionResponse submit(UUID jobId, CurrentUser caller, JobSubmissionRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireSelectedFreelancer(job, caller);
        BugFixJobStatus status = job.getStatus();
        if (status != BugFixJobStatus.ASSIGNED
                && status != BugFixJobStatus.CHANGES_REQUESTED
                && status != BugFixJobStatus.TEST_FAILED) {
            throw new AppException("Job is not open for submission");
        }
        int version = (int) submissionRepository.countByJobId(job.getId()) + 1;
        BugFixJobSubmission submission = new BugFixJobSubmission(
                job.getId(), caller.userId(), request.reportSummary(), request.reproductionDetails(),
                request.fixDetails(), request.evidenceKeys(), version);
        submissionRepository.save(submission);
        transition(job, BugFixJobStatus.SUBMITTED, caller.userId(), "Fix submitted");
        return mapper.toResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<JobSubmissionResponse> listSubmissions(UUID jobId, CurrentUser caller) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanViewJob(job, caller);
        return submissionRepository.findByJobIdOrderBySubmittedAtAsc(job.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public JobReviewResponse recordReview(UUID jobId, CurrentUser caller, JobReviewRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanManageJob(job, caller);
        BugFixJobStatus status = job.getStatus();
        if (status != BugFixJobStatus.SUBMITTED && status != BugFixJobStatus.UNDER_REVIEW) {
            throw new AppException("Job is not awaiting review");
        }
        BugFixJobReview review = new BugFixJobReview(
                job.getId(), caller.userId(), request.outcome(), request.reason(), request.testingEvidenceKeys());
        reviewRepository.save(review);
        BugFixJobStatus target = switch (request.outcome()) {
            case CHANGES_REQUESTED -> BugFixJobStatus.CHANGES_REQUESTED;
            case TEST_FAILED -> BugFixJobStatus.TEST_FAILED;
            case TEST_PASSED -> BugFixJobStatus.TEST_PASSED;
        };
        transition(job, target, caller.userId(), request.reason());
        return mapper.toResponse(review);
    }

    @Transactional(readOnly = true)
    public List<JobReviewResponse> listReviews(UUID jobId, CurrentUser caller) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanViewJob(job, caller);
        return reviewRepository.findByJobIdOrderByCreatedAtAsc(job.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public BugFixJobResponse approveFee(UUID jobId, CurrentUser caller, ApproveFeeRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireOwnerOrAdmin(job, caller);
        if (job.getStatus() != BugFixJobStatus.TEST_PASSED) {
            throw new AppException("Fee can only be approved after testing passed");
        }
        if (request.amount().compareTo(job.getFee()) != 0
                || !request.currency().equalsIgnoreCase(job.getCurrency())) {
            throw new ConflictException("Approved amount must equal the agreed fixed fee");
        }
        transition(job, BugFixJobStatus.FEE_APPROVED, caller.userId(), request.note());
        return mapper.toResponse(job);
    }

    @Transactional
    public BugFixJobResponse recordPayment(UUID jobId, CurrentUser caller, RecordPaymentRequest request) {
        BugFixJob job = load(jobId);
        // Until the dedicated Payment service exists, the company owner/admin drives this seam.
        jobAuthorization.requireOwnerOrAdmin(job, caller);
        PaymentStage stage = request.stage();

        if (stage == PaymentStage.WAITING_FOR_COMPANY_PAYMENT) {
            if (job.getStatus() == BugFixJobStatus.WAITING_FOR_COMPANY_PAYMENT
                    && Objects.equals(job.getPaymentReference(), request.paymentReference())) {
                return mapper.toResponse(job); // idempotent replay
            }
            job.setPaymentId(request.paymentId());
            job.setPaymentReference(request.paymentReference());
            transition(job, BugFixJobStatus.WAITING_FOR_COMPANY_PAYMENT, caller.userId(), "Awaiting company payment");
        } else if (stage == PaymentStage.PAYMENT_VERIFIED) {
            if (job.getStatus() == BugFixJobStatus.PAYMENT_VERIFIED
                    && Objects.equals(job.getPaymentReference(), request.paymentReference())) {
                return mapper.toResponse(job); // idempotent replay
            }
            if (request.paymentId() != null) {
                job.setPaymentId(request.paymentId());
            }
            job.setPaymentReference(request.paymentReference());
            job.setVerifiedAmount(request.amount());
            job.setVerifiedCurrency(request.currency());
            transition(job, BugFixJobStatus.PAYMENT_VERIFIED, caller.userId(), "Payment verified");
        } else {
            throw new AppException("Unsupported payment stage");
        }
        return mapper.toResponse(job);
    }

    @Transactional
    public BugFixJobResponse confirmReceipt(UUID jobId, CurrentUser caller, ConfirmReceiptRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireSelectedFreelancer(job, caller);
        if (job.getStatus() != BugFixJobStatus.PAYMENT_VERIFIED) {
            throw new AppException("Cannot confirm receipt before payment is verified");
        }
        String note = request != null ? request.note() : null;
        transition(job, BugFixJobStatus.FEE_RECEIPT_CONFIRMED, caller.userId(), note);
        transition(job, BugFixJobStatus.CLOSED, caller.userId(), note);
        return mapper.toResponse(job);
    }

    @Transactional
    public BugFixJobResponse close(UUID jobId, CurrentUser caller, JobActionRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanManageJob(job, caller);
        if (job.getStatus() != BugFixJobStatus.PAYMENT_VERIFIED
                && job.getStatus() != BugFixJobStatus.FEE_RECEIPT_CONFIRMED) {
            throw new AppException("Job can only be closed after payment is verified or receipt confirmed");
        }
        transition(job, BugFixJobStatus.CLOSED, caller.userId(), reason(request));
        return mapper.toResponse(job);
    }

    @Transactional
    public BugFixJobResponse cancel(UUID jobId, CurrentUser caller, JobActionRequest request) {
        BugFixJob job = load(jobId);
        jobAuthorization.requireCanManageJob(job, caller);
        String reason = reason(request);
        if (reason == null || reason.isBlank()) {
            throw new AppException("A reason is required to cancel a job");
        }
        // The transition map only permits ->CANCELLED before FEE_APPROVED (and from DISPUTED).
        transition(job, BugFixJobStatus.CANCELLED, caller.userId(), reason);
        return mapper.toResponse(job);
    }

    @Transactional
    public BugFixJobResponse openDispute(UUID jobId, CurrentUser caller, JobActionRequest request) {
        BugFixJob job = load(jobId);
        try {
            jobAuthorization.requireCanManageJob(job, caller);
        } catch (com.workora.project.exception.ForbiddenException notManager) {
            jobAuthorization.requireSelectedFreelancer(job, caller);
        }
        String reason = reason(request);
        if (reason == null || reason.isBlank()) {
            throw new AppException("A reason is required to open a dispute");
        }
        // The transition map only permits ->DISPUTED before FEE_RECEIPT_CONFIRMED.
        transition(job, BugFixJobStatus.DISPUTED, caller.userId(), reason);
        return mapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public BugFixJob load(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Bug-Fix Job not found"));
    }

    /** Validates the move against {@link #ALLOWED_TRANSITIONS}, stamps lifecycle times, persists and records history. */
    void transition(BugFixJob job, BugFixJobStatus target, UUID actorId, String reason) {
        BugFixJobStatus current = job.getStatus();
        Set<BugFixJobStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new AppException("Invalid status transition from " + current + " to " + target);
        }
        job.setStatus(target);
        if (target == BugFixJobStatus.OPEN && job.getPublishedAt() == null) {
            job.setPublishedAt(Instant.now());
        }
        if (target == BugFixJobStatus.CLOSED) {
            job.setClosedAt(Instant.now());
        }
        jobRepository.save(job);
        historyRepository.save(new BugFixJobStatusHistory(job.getId(), current, target, actorId, reason));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", job.getId());
        if (job.getPhaseId() != null) {
            payload.put("phaseId", job.getPhaseId());
        }
        if (current != null) {
            payload.put("from", current.name());
        }
        if (target != null) {
            payload.put("to", target.name());
        }
        if (reason != null) {
            payload.put("reason", reason);
        }
        projectEventPublisher.publish(
                "job.status.changed",
                job.getProjectId(),
                job.getCompanyId(),
                actorId,
                job.getSelectedFreelancerId(),
                payload
        );
    }

    private String reason(JobActionRequest request) {
        return request != null ? request.reason() : null;
    }
}
