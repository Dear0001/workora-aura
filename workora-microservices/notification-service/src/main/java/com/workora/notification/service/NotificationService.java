package com.workora.notification.service;

import com.workora.common.dto.PageResponse;
import com.workora.notification.dto.NotificationCreateRequest;
import com.workora.notification.dto.NotificationResponse;
import com.workora.notification.dto.UnreadCountResponse;
import com.workora.notification.entity.DeliveryAttempt;
import com.workora.notification.entity.Notification;
import com.workora.notification.entity.NotificationChannel;
import com.workora.notification.entity.NotificationStatus;
import com.workora.notification.exception.ForbiddenException;
import com.workora.notification.exception.NotFoundException;
import com.workora.notification.mapper.NotificationMapper;
import com.workora.notification.repository.DeliveryAttemptRepository;
import com.workora.notification.repository.NotificationRepository;
import com.workora.notification.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final NotificationMapper mapper;
    private final String internalServiceToken;

    public NotificationService(NotificationRepository notificationRepository,
                               DeliveryAttemptRepository deliveryAttemptRepository,
                               NotificationMapper mapper,
                               @Value("${workora.notifications.internal-service-token:}") String internalServiceToken) {
        this.notificationRepository = notificationRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.mapper = mapper;
        this.internalServiceToken = internalServiceToken;
    }

    public NotificationResponse create(CurrentUser caller, NotificationCreateRequest req) {
        return create(caller, req, internalServiceToken);
    }

    public NotificationResponse create(CurrentUser caller, NotificationCreateRequest req, String providedToken) {
        if (internalServiceToken == null || internalServiceToken.isBlank()) {
            throw new ForbiddenException("Trusted internal service credentials are not configured");
        }
        if (providedToken == null || !internalServiceToken.equals(providedToken)) {
            throw new ForbiddenException("Trusted internal service credentials are required to create notifications");
        }

        Notification notification = new Notification(
                req.userId(), req.type(), req.title(), req.body(),
                req.relatedEntityType(), req.relatedEntityId(), req.recipientEmail());
        notification = notificationRepository.save(notification);

        deliveryAttemptRepository.save(
                new DeliveryAttempt(notification.getId(), NotificationChannel.IN_APP, "SENT", null));
        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setDeliveredAt(Instant.now());
        if (req.recipientEmail() != null && !req.recipientEmail().isBlank()) {
            deliveryAttemptRepository.save(
                    new DeliveryAttempt(notification.getId(), NotificationChannel.EMAIL, "PENDING", null));
        }
        return mapper.toResponse(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> inbox(CurrentUser caller, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                        caller.userId(), NotificationStatus.DELIVERED, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(caller.userId(), pageable);
        return PageResponse.of(
                page.getContent().stream().map(mapper::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID notificationId, CurrentUser caller) {
        return mapper.toResponse(loadOwn(notificationId, caller));
    }

    public NotificationResponse markRead(UUID notificationId, CurrentUser caller) {
        Notification notification = loadOwn(notificationId, caller);
        if (notification.getStatus() != NotificationStatus.READ
                && notification.getStatus() != NotificationStatus.ARCHIVED) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(Instant.now());
        }
        return mapper.toResponse(notificationRepository.save(notification));
    }

    public NotificationResponse archive(UUID notificationId, CurrentUser caller) {
        Notification notification = loadOwn(notificationId, caller);
        notification.setStatus(NotificationStatus.ARCHIVED);
        notification.setArchivedAt(Instant.now());
        return mapper.toResponse(notificationRepository.save(notification));
    }

    public void delete(UUID notificationId, CurrentUser caller) {
        Notification notification = loadOwn(notificationId, caller);
        notificationRepository.delete(notification);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(CurrentUser caller) {
        return new UnreadCountResponse(
                notificationRepository.countByUserIdAndStatus(caller.userId(), NotificationStatus.DELIVERED));
    }

    /** 404 for foreign notifications so existence is never leaked to non-owners. */
    private Notification loadOwn(UUID notificationId, CurrentUser caller) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!notification.getUserId().equals(caller.userId())) {
            throw new NotFoundException("Notification not found");
        }
        return notification;
    }
}
