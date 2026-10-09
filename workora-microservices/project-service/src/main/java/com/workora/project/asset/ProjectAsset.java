package com.workora.project.asset;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project_assets", indexes = {
        @Index(name = "idx_project_asset_project", columnList = "projectId"),
        @Index(name = "idx_project_asset_phase", columnList = "phaseId")
})
public class ProjectAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID projectId;

    private UUID phaseId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private UUID uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ProjectAsset() {
    }

    public ProjectAsset(UUID projectId, UUID phaseId, String name, String storageKey, String contentType, String kind, UUID uploadedBy) {
        this.projectId = projectId;
        this.phaseId = phaseId;
        this.name = name;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.kind = kind;
        this.uploadedBy = uploadedBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProjectId() { return projectId; }
    public UUID getPhaseId() { return phaseId; }
    public String getName() { return name; }
    public String getStorageKey() { return storageKey; }
    public String getContentType() { return contentType; }
    public String getKind() { return kind; }
    public UUID getUploadedBy() { return uploadedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
