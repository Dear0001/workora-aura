package com.workora.payment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_profiles", uniqueConstraints = @UniqueConstraint(
        name = "uk_payment_profile_owner_provider", columnNames = {"ownerId", "provider"}))
public class PaymentProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false) private UUID ownerId;
    @Column(nullable = false, length = 30) private String provider;
    @Column(nullable = false) private String walletName;
    @Column(nullable = false, length = 40) private String phoneNumber;
    @Column(nullable = false) private boolean primaryProfile;
    @Column(length = 4000) private String qrPayload;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected PaymentProfile() {}
    public PaymentProfile(UUID ownerId, String provider, String walletName, String phoneNumber, boolean primaryProfile) {
        this.ownerId = ownerId; this.provider = provider; this.walletName = walletName;
        this.phoneNumber = phoneNumber; this.primaryProfile = primaryProfile;
    }
    @PrePersist void create() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void update() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getProvider() { return provider; }
    public String getWalletName() { return walletName; }
    public void setWalletName(String value) { walletName = value; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String value) { phoneNumber = value; }
    public boolean isPrimaryProfile() { return primaryProfile; }
    public void setPrimaryProfile(boolean value) { primaryProfile = value; }
    public String getQrPayload() { return qrPayload; }
    public void setQrPayload(String value) { qrPayload = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
