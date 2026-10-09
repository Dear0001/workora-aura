package com.workora.bug.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bug_status_history", indexes = {
        @Index(name = "idx_bug_history_report", columnList = "reportId")
})
public class BugStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID reportId;

    @Enumerated(EnumType.STRING)
    private BugReportStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugReportStatus toStatus;

    @Column(nullable = false)
    private UUID actorId;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    protected BugStatusHistory() {
    }

    public BugStatusHistory(UUID reportId, BugReportStatus fromStatus, BugReportStatus toStatus,
                            UUID actorId, String reason) {
        this.reportId = reportId;
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

    public UUID getReportId() {
        return reportId;
    }

    public BugReportStatus getFromStatus() {
        return fromStatus;
    }

    public BugReportStatus getToStatus() {
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
