package com.workora.notification.repository;

import com.workora.notification.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import com.workora.notification.entity.NotificationChannel;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    List<DeliveryAttempt> findByNotificationId(UUID notificationId);

    List<DeliveryAttempt> findByChannelAndStatus(NotificationChannel channel, String status);

    List<DeliveryAttempt> findByChannelAndStatusIn(NotificationChannel channel, List<String> statuses);

    java.util.Optional<DeliveryAttempt> findFirstByNotificationIdAndChannelOrderByAttemptedAtDesc(
            UUID notificationId, NotificationChannel channel);
}
