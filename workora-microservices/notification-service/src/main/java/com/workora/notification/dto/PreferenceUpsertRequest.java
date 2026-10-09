package com.workora.notification.dto;

import com.workora.notification.entity.NotificationChannel;
import com.workora.notification.entity.NotificationType;
import jakarta.validation.constraints.NotNull;

public record PreferenceUpsertRequest(
        @NotNull NotificationChannel channel,
        @NotNull NotificationType type,
        @NotNull Boolean enabled
) {
}
