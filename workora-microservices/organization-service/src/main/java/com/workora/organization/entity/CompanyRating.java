package com.workora.organization.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "company_ratings", indexes = {
        @Index(name = "idx_rating_company_user", columnList = "companyId,userId", unique = true),
        @Index(name = "idx_rating_company_created", columnList = "companyId,createdAt")
})
public class CompanyRating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Column(length = 2000)
    private String review;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected CompanyRating() {
    }

    public CompanyRating(UUID companyId, UUID userId, int score, String review) {
        this.companyId = companyId;
        this.userId = userId;
        this.score = score;
        this.review = review;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
