package com.workora.organization.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership_audit", indexes = {
        @Index(name = "idx_membership_audit_company_user", columnList = "companyId,userId"),
        @Index(name = "idx_membership_audit_actor", columnList = "actorUserId")
})
public class MembershipAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID actorUserId;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    private CompanyRole roleBefore;

    @Enumerated(EnumType.STRING)
    private CompanyRole roleAfter;

    @Column(length = 2000)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MembershipAudit() {
    }

    public MembershipAudit(UUID companyId, UUID userId, UUID actorUserId, String action,
                          CompanyRole roleBefore, CompanyRole roleAfter, String details) {
        this.companyId = companyId;
        this.userId = userId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.roleBefore = roleBefore;
        this.roleAfter = roleAfter;
        this.details = details;
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

    public UUID getUserId() {
        return userId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public CompanyRole getRoleBefore() {
        return roleBefore;
    }

    public CompanyRole getRoleAfter() {
        return roleAfter;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
