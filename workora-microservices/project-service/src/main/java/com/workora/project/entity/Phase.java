package com.workora.project.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phases", indexes = {
        @Index(name = "idx_phase_project", columnList = "projectId"),
        @Index(name = "idx_phase_public", columnList = "status,visibility")
})
public class Phase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    @Column(length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseVisibility visibility = PhaseVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhaseStatus status = PhaseStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant publishedAt;

    private Instant closedAt;

    protected Phase() {
    }

    public Phase(UUID projectId, String name) {
        this.projectId = projectId;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PhaseVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(PhaseVisibility visibility) {
        this.visibility = visibility;
    }

    public PhaseStatus getStatus() {
        return status;
    }

    public void setStatus(PhaseStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }
}
