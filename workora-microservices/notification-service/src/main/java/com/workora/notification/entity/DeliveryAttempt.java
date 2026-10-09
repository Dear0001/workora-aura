package com.workora.notification.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "delivery_attempts", indexes = {
        @Index(name = "idx_delivery_notification", columnList = "notificationId")
})
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    /** PENDING / SENT / FAILED. Email delivery is deferred; IN_APP is recorded as SENT immediately. */
    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Instant attemptedAt;

    @Column(length = 1000)
    private String error;

    protected DeliveryAttempt() {
    }

    public DeliveryAttempt(UUID notificationId, NotificationChannel channel, String status, String error) {
        this.notificationId = notificationId;
        this.channel = channel;
        this.status = status;
        this.error = error;
        this.attemptedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getNotificationId() {
        return notificationId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getStatus() {
        return status;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public String getError() {
        return error;
    }

    public void mark(String status, String error) {
        this.status = status;
        this.error = error;
        this.attemptedAt = Instant.now();
    }
}
