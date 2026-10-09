package com.workora.notification.mapper;

import com.workora.notification.dto.NotificationResponse;
import com.workora.notification.dto.PreferenceResponse;
import com.workora.notification.entity.Notification;
import com.workora.notification.entity.NotificationPreference;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getDeliveredAt(),
                notification.getReadAt(),
                notification.getArchivedAt()
        );
    }

    public PreferenceResponse toResponse(NotificationPreference preference) {
        return new PreferenceResponse(
                preference.getId(),
                preference.getUserId(),
                preference.getChannel(),
                preference.getType(),
                preference.isEnabled(),
                preference.getUpdatedAt()
        );
    }
}
