package com.workora.notification.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uq_preference_user_channel_type", columnNames = {"userId", "channel", "type"})
}, indexes = {
        @Index(name = "idx_preference_user", columnList = "userId")
})
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private Instant updatedAt;

    protected NotificationPreference() {
    }

    public NotificationPreference(UUID userId, NotificationChannel channel, NotificationType type, boolean enabled) {
        this.userId = userId;
        this.channel = channel;
        this.type = type;
        this.enabled = enabled;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
