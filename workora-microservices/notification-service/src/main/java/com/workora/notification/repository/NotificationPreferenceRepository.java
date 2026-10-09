package com.workora.notification.repository;

import com.workora.notification.entity.NotificationChannel;
import com.workora.notification.entity.NotificationPreference;
import com.workora.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndChannelAndType(UUID userId, NotificationChannel channel, NotificationType type);
}
