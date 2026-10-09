package com.workora.notification.dto;

import com.workora.notification.entity.NotificationStatus;
import com.workora.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String body,
        String relatedEntityType,
        UUID relatedEntityId,
        NotificationStatus status,
        Instant createdAt,
        Instant deliveredAt,
        Instant readAt,
        Instant archivedAt
) {
}
