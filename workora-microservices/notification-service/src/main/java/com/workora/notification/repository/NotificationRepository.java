package com.workora.notification.repository;

import com.workora.notification.entity.Notification;
import com.workora.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, NotificationStatus status, Pageable pageable);

    long countByUserIdAndStatus(UUID userId, NotificationStatus status);
}
