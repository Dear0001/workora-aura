package com.workora.organization.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", uniqueConstraints = @UniqueConstraint(name = "uk_subscription_user", columnNames = "userId"))
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Column(nullable = false)
    private int maxCompanies = 1;

    @Column(nullable = false)
    private boolean entitled = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Subscription() {}

    public Subscription(UUID userId) {
        this.userId = userId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }
    public int getMaxCompanies() { return maxCompanies; }
    public void setMaxCompanies(int maxCompanies) { this.maxCompanies = maxCompanies; }
    public boolean isEntitled() { return entitled; }
    public void setEntitled(boolean entitled) { this.entitled = entitled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
