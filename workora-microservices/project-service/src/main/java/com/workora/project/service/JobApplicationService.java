package com.workora.project.service;

import com.workora.project.dto.JobApplicationRequest;
import com.workora.project.dto.JobApplicationResponse;
import com.workora.project.entity.BugFixJob;
import com.workora.project.entity.BugFixJobApplication;
import com.workora.project.entity.BugFixJobStatus;
import com.workora.project.entity.JobApplicationDecision;
import com.workora.project.exception.AppException;
import com.workora.project.exception.ConflictException;
import com.workora.project.exception.NotFoundException;
import com.workora.project.mapper.ProjectMapper;
import com.workora.project.repository.BugFixJobApplicationRepository;
import com.workora.project.repository.BugFixJobRepository;
import com.workora.project.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JobApplicationService {

    private final BugFixJobApplicationRepository applicationRepository;
    private final BugFixJobRepository jobRepository;
    private final BugFixJobAuthorization jobAuthorization;
    private final ProjectMapper mapper;

    public JobApplicationService(BugFixJobApplicationRepository applicationRepository,
                                 BugFixJobRepository jobRepository,
                                 BugFixJobAuthorization jobAuthorization,
                                 ProjectMapper mapper) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.jobAuthorization = jobAuthorization;
        this.mapper = mapper;
    }

    @Transactional
    public JobApplicationResponse apply(UUID jobId, CurrentUser caller, JobApplicationRequest request) {
        BugFixJob job = loadJob(jobId);
        jobAuthorization.requireNotOwnCompany(job, caller);
        if (job.getStatus() != BugFixJobStatus.OPEN) {
            throw new AppException("Job is not accepting applications");
        }
        if (applicationRepository.findByJobIdAndApplicantId(jobId, caller.userId()).isPresent()) {
            throw new ConflictException("Already applied");
        }
        BugFixJobApplication application = new BugFixJobApplication(
                jobId, caller.userId(), caller.email(), request != null ? request.proposal() : null);
        return mapper.toResponse(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<JobApplicationResponse> listForJob(UUID jobId, CurrentUser caller) {
        BugFixJob job = loadJob(jobId);
        jobAuthorization.requireCanManageJob(job, caller);
        return applicationRepository.findByJobIdOrderByCreatedAtAsc(jobId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public void withdraw(UUID jobId, CurrentUser caller) {
        BugFixJob job = loadJob(jobId);
        BugFixJobApplication application = applicationRepository
                .findByJobIdAndApplicantId(jobId, caller.userId())
                .orElseThrow(() -> new NotFoundException("Application not found"));
        if (application.getDecision() != JobApplicationDecision.PENDING
                || job.getStatus() != BugFixJobStatus.OPEN) {
            throw new AppException("Cannot withdraw");
        }
        application.setDecision(JobApplicationDecision.WITHDRAWN);
        application.setDecidedAt(Instant.now());
        applicationRepository.save(application);
    }

    private BugFixJob loadJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Bug-Fix Job not found"));
    }
}
