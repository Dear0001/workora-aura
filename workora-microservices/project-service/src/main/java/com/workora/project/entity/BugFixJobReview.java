package com.workora.project.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bug_fix_job_reviews", indexes = {
        @Index(name = "idx_bfjr_job", columnList = "jobId")
})
public class BugFixJobReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Column(nullable = false)
    private UUID reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewOutcome outcome;

    @Column(length = 2000)
    private String reason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "bug_fix_job_review_evidence", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "evidence_key", length = 500)
    private List<String> testingEvidenceKeys = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected BugFixJobReview() {
    }

    public BugFixJobReview(UUID jobId, UUID reviewerId, ReviewOutcome outcome, String reason,
                           List<String> testingEvidenceKeys) {
        this.jobId = jobId;
        this.reviewerId = reviewerId;
        this.outcome = outcome;
        this.reason = reason;
        this.testingEvidenceKeys = testingEvidenceKeys != null
                ? new ArrayList<>(testingEvidenceKeys) : new ArrayList<>();
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

    public UUID getReviewerId() {
        return reviewerId;
    }

    public ReviewOutcome getOutcome() {
        return outcome;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getTestingEvidenceKeys() {
        return testingEvidenceKeys;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
