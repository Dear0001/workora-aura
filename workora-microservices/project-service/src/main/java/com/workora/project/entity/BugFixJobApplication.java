package com.workora.project.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bug_fix_job_applications", indexes = {
        @Index(name = "idx_bfja_job", columnList = "jobId"),
        @Index(name = "idx_bfja_applicant", columnList = "applicantId")
})
public class BugFixJobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID applicantId;

    private String applicantEmail;

    @Column(length = 2000)
    private String proposal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobApplicationDecision decision = JobApplicationDecision.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant decidedAt;

    protected BugFixJobApplication() {
    }

    public BugFixJobApplication(UUID jobId, UUID applicantId, String applicantEmail, String proposal) {
        this.jobId = jobId;
        this.applicantId = applicantId;
        this.applicantEmail = applicantEmail;
        this.proposal = proposal;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getApplicantId() {
        return applicantId;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public String getProposal() {
        return proposal;
    }

    public JobApplicationDecision getDecision() {
        return decision;
    }

    public void setDecision(JobApplicationDecision decision) {
        this.decision = decision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }
}
