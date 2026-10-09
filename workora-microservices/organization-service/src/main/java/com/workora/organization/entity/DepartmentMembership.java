package com.workora.organization.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "department_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_department_membership_member_department",
                columnNames = {"membership_id", "departmentId"}),
        indexes = {
                @Index(name = "idx_department_membership_department", columnList = "departmentId")
        })
public class DepartmentMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;

    @Column(nullable = false)
    private UUID departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepartmentRole role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected DepartmentMembership() {
    }

    public DepartmentMembership(Membership membership, UUID departmentId, DepartmentRole role) {
        this.membership = membership;
        this.departmentId = departmentId;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Membership getMembership() {
        return membership;
    }

    public UUID getDepartmentId() {
        return departmentId;
    }

    public DepartmentRole getRole() {
        return role;
    }

    public void setRole(DepartmentRole role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
