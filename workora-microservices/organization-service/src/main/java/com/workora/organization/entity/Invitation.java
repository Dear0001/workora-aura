package com.workora.organization.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitations", indexes = {
        @Index(name = "idx_invitation_company", columnList = "companyId"),
        @Index(name = "idx_invitation_token", columnList = "token")
})
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    /** Registered account email the invitation was sent to. */
    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyRole role;

    private UUID departmentId;

    /** Single-use, expiring accept token. */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private UUID invitedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant acceptedAt;

    private Instant revokedAt;

    protected Invitation() {
    }

    public Invitation(UUID companyId, String email, CompanyRole role, UUID departmentId,
                      String token, UUID invitedBy, Instant expiresAt) {
        this.companyId = companyId;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
        this.token = token;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getEmail() {
        return email;
    }

    public CompanyRole getRole() {
        return role;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public String getToken() {
        return token;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
