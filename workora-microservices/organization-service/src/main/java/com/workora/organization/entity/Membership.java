package com.workora.organization.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "memberships", indexes = {
        @Index(name = "idx_membership_company_user", columnList = "companyId,userId"),
        @Index(name = "idx_membership_department", columnList = "departmentId")
})
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    /** Keycloak subject id of the member. */
    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    private UUID departmentId;

    private UUID invitedBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "membership_permissions", joinColumns = @JoinColumn(name = "membership_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<CompanyPermission> permissions = EnumSet.noneOf(CompanyPermission.class);

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant removedAt;

    protected Membership() {
    }

    public Membership(UUID companyId, UUID userId, String email, CompanyRole role, UUID departmentId, UUID invitedBy) {
        this.companyId = companyId;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.departmentId = departmentId;
        this.invitedBy = invitedBy;
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

    public UUID getCompanyId() {
        return companyId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CompanyRole getRole() {
        return role;
    }

    public void setRole(CompanyRole role) {
        this.role = role;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(UUID departmentId) {
        this.departmentId = departmentId;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public Set<CompanyPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<CompanyPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            this.permissions = EnumSet.noneOf(CompanyPermission.class);
            return;
        }
        this.permissions = EnumSet.copyOf(permissions);
    }

    public boolean hasPermission(CompanyPermission permission) {
        return permissions != null && permissions.contains(permission);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(Instant removedAt) {
        this.removedAt = removedAt;
    }
}
