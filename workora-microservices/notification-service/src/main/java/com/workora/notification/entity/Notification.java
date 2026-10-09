package com.workora.notification.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "userId"),
        @Index(name = "idx_notification_user_status", columnList = "userId,status")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Recipient user id (Keycloak subject). */
    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String body;

    @Column(length = 64)
    private String relatedEntityType;

    private UUID relatedEntityId;

    @Column(length = 320)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status = NotificationStatus.CREATED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant deliveredAt;

    private Instant readAt;

    private Instant archivedAt;

    protected Notification() {
    }

    public Notification(UUID userId, NotificationType type, String title, String body,
                        String relatedEntityType, UUID relatedEntityId) {
        this(userId, type, title, body, relatedEntityType, relatedEntityId, null);
    }

    public Notification(UUID userId, NotificationType type, String title, String body,
                        String relatedEntityType, UUID relatedEntityId, String recipientEmail) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.recipientEmail = recipientEmail;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public UUID getRelatedEntityId() {
        return relatedEntityId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }
}
