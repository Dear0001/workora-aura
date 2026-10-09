package com.workora.identity.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_access_requests", indexes = {
        @Index(name = "idx_admin_access_request_status_created", columnList = "status,createdAt")
})
public class AdminAccessRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 320)
    private String requestedBy;

    @Column(length = 320)
    private String decidedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AdminAccessRequestStatus status = AdminAccessRequestStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant decidedAt;

    protected AdminAccessRequest() {}

    public AdminAccessRequest(String email, String requestedBy) {
        this.email = email;
        this.requestedBy = requestedBy;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void decide(String actor, AdminAccessRequestStatus result) {
        status = result;
        decidedBy = actor;
        decidedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getRequestedBy() { return requestedBy; }
    public String getDecidedBy() { return decidedBy; }
    public AdminAccessRequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
