package com.workora.identity.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration_verification_sessions", indexes = {
        @Index(name = "idx_registration_verification_token_hash", columnList = "tokenHash", unique = true)
})
public class RegistrationVerificationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant verifiedAt;

    protected RegistrationVerificationSession() {
    }

    public RegistrationVerificationSession(String tokenHash, UUID userId, String email,
                                           Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.email = email;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void markVerified(Instant at) {
        this.verifiedAt = at;
    }
}
