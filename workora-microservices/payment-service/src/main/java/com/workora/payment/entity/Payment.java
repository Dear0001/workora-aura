package com.workora.payment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_payer_created", columnList = "payerId,createdAt"),
        @Index(name = "idx_payments_status", columnList = "status")
})
public class Payment {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID payerId;
    @Column(nullable = false, unique = true, length = 120)
    private String reference;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(nullable = false)
    private String purpose;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    @Column(nullable = false)
    private String provider;
    private String providerReference;
    @Column(length = 4000)
    private String qrPayload;
    @Column(length = 2000)
    private String deepLink;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant paidAt;
    private Instant receiptConfirmedAt;
    @Column(length = 2000)
    private String disputeReason;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected Payment() {}

    public Payment(UUID payerId, String reference, BigDecimal amount, String currency,
                   String purpose, String provider, String qrPayload, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.payerId = payerId;
        this.reference = reference;
        this.amount = amount;
        this.currency = currency;
        this.purpose = purpose;
        this.provider = provider;
        this.qrPayload = qrPayload;
        this.expiresAt = expiresAt;
        this.status = PaymentStatus.CREATED;
    }

    @PrePersist
    void create() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate
    void update() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getPayerId() { return payerId; }
    public String getReference() { return reference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getPurpose() { return purpose; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getProvider() { return provider; }
    public String getProviderReference() { return providerReference; }
    public void setProviderReference(String value) { providerReference = value; }
    public String getQrPayload() { return qrPayload; }
    public void setDeepLink(String value) { deepLink = value; }
    public String getDeepLink() { return deepLink; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant value) { paidAt = value; }
    public Instant getReceiptConfirmedAt() { return receiptConfirmedAt; }
    public void setReceiptConfirmedAt(Instant value) { receiptConfirmedAt = value; }
    public String getDisputeReason() { return disputeReason; }
    public void setDisputeReason(String value) { disputeReason = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
