package com.workora.recruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_history")
public class ApplicationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus toStatus;

    @Column(nullable = false)
    private UUID actorId;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    void onCreate() {
        this.changedAt = Instant.now();
    }

    public ApplicationHistory() {
    }

    public ApplicationHistory(UUID applicationId, ApplicationStatus fromStatus,
                              ApplicationStatus toStatus, UUID actorId, String reason) {
        this.applicationId = applicationId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
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
