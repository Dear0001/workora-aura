package com.workora.bug.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * PRIVATE attachment metadata for a bug report. Evidence must never be projected in
 * public/list responses; access is gated by {@code BugAuthorization}.
 */
@Entity
@Table(name = "bug_evidence", indexes = {
        @Index(name = "idx_bug_evidence_report", columnList = "reportId")
})
public class BugEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID reportId;

    @Column(nullable = false)
    private String fileKey;

    private String mimeType;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;

    protected BugEvidence() {
    }

    public BugEvidence(UUID reportId, String fileKey, String mimeType, String description, UUID uploadedBy) {
        this.reportId = reportId;
        this.fileKey = fileKey;
        this.mimeType = mimeType;
        this.description = description;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        this.uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getReportId() {
        return reportId;
    }

    public String getFileKey() {
        return fileKey;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDescription() {
        return description;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
