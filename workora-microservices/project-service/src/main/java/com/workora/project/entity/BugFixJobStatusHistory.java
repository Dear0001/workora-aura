package com.workora.project.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only audit trail of every job status transition, including the actor and an
 * optional human reason. {@code fromStatus} is null for the initial creation entry.
 */
@Entity
@Table(name = "bug_fix_job_status_history", indexes = {
        @Index(name = "idx_bfjsh_job", columnList = "jobId")
})
public class BugFixJobStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    private BugFixJobStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugFixJobStatus toStatus;

    @Column(nullable = false)
    private UUID actorId;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    protected BugFixJobStatusHistory() {
    }

    public BugFixJobStatusHistory(UUID jobId, BugFixJobStatus fromStatus, BugFixJobStatus toStatus,
                                  UUID actorId, String reason) {
        this.jobId = jobId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        this.changedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getJobId() {
        return jobId;
    }

    public BugFixJobStatus getFromStatus() {
        return fromStatus;
    }

    public BugFixJobStatus getToStatus() {
        return toStatus;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
