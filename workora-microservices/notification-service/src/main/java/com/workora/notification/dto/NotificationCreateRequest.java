package com.workora.notification.dto;

import com.workora.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

import java.util.UUID;

public record NotificationCreateRequest(
        @NotNull UUID userId,
        @NotNull NotificationType type,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String body,
        @Size(max = 64) String relatedEntityType,
        UUID relatedEntityId,
        @Email @Size(max = 320) String recipientEmail
) {
    public NotificationCreateRequest(UUID userId, NotificationType type, String title, String body,
                                     String relatedEntityType, UUID relatedEntityId) {
        this(userId, type, title, body, relatedEntityType, relatedEntityId, null);
    }
}
