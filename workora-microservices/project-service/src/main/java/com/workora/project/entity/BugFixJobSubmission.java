package com.workora.project.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A freelancer's fix + evidence for a job. Immutable after creation: every resubmission
 * is a new row with an incremented {@code version}, preserving the full audit trail.
 */
@Entity
@Table(name = "bug_fix_job_submissions", indexes = {
        @Index(name = "idx_bfjs_job", columnList = "jobId")
})
public class BugFixJobSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID freelancerId;

    @Column(length = 4000)
    private String reportSummary;

    @Column(length = 4000)
    private String reproductionDetails;

    @Column(length = 4000)
    private String fixDetails;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bug_fix_job_submission_evidence", joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "evidence_key", length = 500)
    private List<String> evidenceKeys = new ArrayList<>();

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    protected BugFixJobSubmission() {
    }

    public BugFixJobSubmission(UUID jobId, UUID freelancerId, String reportSummary,
                               String reproductionDetails, String fixDetails,
                               List<String> evidenceKeys, int version) {
        this.jobId = jobId;
        this.freelancerId = freelancerId;
        this.reportSummary = reportSummary;
        this.reproductionDetails = reproductionDetails;
        this.fixDetails = fixDetails;
        this.evidenceKeys = evidenceKeys != null ? new ArrayList<>(evidenceKeys) : new ArrayList<>();
        this.version = version;
    }

    @PrePersist
    void onCreate() {
        this.submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public UUID getFreelancerId() {
        return freelancerId;
    }

    public String getReportSummary() {
        return reportSummary;
    }

    public String getReproductionDetails() {
        return reproductionDetails;
    }

    public String getFixDetails() {
        return fixDetails;
    }

    public List<String> getEvidenceKeys() {
        return evidenceKeys;
    }

    public int getVersion() {
        return version;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
