package com.workora.project.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phase_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_phase_member", columnNames = {"phaseId", "userId"})
})
public class PhaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID phaseId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private UUID addedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected PhaseMember() {
    }

    public PhaseMember(UUID phaseId, UUID userId, String email, UUID addedBy) {
        this.phaseId = phaseId;
        this.userId = userId;
        this.email = email;
        this.addedBy = addedBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPhaseId() {
        return phaseId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UUID getAddedBy() {
        return addedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
