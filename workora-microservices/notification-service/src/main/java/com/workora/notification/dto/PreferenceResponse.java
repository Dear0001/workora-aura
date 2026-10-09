package com.workora.notification.dto;

import com.workora.notification.entity.NotificationChannel;
import com.workora.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record PreferenceResponse(
        UUID id,
        UUID userId,
        NotificationChannel channel,
        NotificationType type,
        boolean enabled,
        Instant updatedAt
) {
}
