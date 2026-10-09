package com.workora.notification.service;

import com.workora.notification.dto.NotificationCreateRequest;
import com.workora.notification.dto.NotificationResponse;
import com.workora.notification.dto.UnreadCountResponse;
import com.workora.notification.entity.DeliveryAttempt;
import com.workora.notification.entity.Notification;
import com.workora.notification.entity.NotificationChannel;
import com.workora.notification.entity.NotificationStatus;
import com.workora.notification.entity.NotificationType;
import com.workora.notification.exception.NotFoundException;
import com.workora.notification.mapper.NotificationMapper;
import com.workora.notification.repository.DeliveryAttemptRepository;
import com.workora.notification.repository.NotificationRepository;
import com.workora.notification.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeliveryAttemptRepository deliveryAttemptRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(
                notificationRepository,
                deliveryAttemptRepository,
                new NotificationMapper(),
                "test-internal-token");
    }

    @Test
    void create_persistsNotificationAndRecordsInAppSentDeliveryAttempt() {
        UUID recipientId = UUID.randomUUID();
        CurrentUser caller = new CurrentUser(UUID.randomUUID(), "sender@acme.test");
        NotificationCreateRequest request = new NotificationCreateRequest(
                recipientId, NotificationType.TASK_ASSIGNED, "New task", "You have a new task",
                "task", UUID.randomUUID());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = service.create(caller, request, "test-internal-token");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification saved = notificationCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(recipientId);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(saved.getDeliveredAt()).isNotNull();

        ArgumentCaptor<DeliveryAttempt> attemptCaptor = ArgumentCaptor.forClass(DeliveryAttempt.class);
        verify(deliveryAttemptRepository).save(attemptCaptor.capture());
        DeliveryAttempt attempt = attemptCaptor.getValue();
        assertThat(attempt.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(attempt.getStatus()).isEqualTo("SENT");

        assertThat(response.status()).isEqualTo(NotificationStatus.DELIVERED);
        assertThat(response.title()).isEqualTo("New task");
    }

    @Test
    void create_requiresTrustedInternalServiceToken() {
        UUID recipientId = UUID.randomUUID();
        CurrentUser caller = new CurrentUser(UUID.randomUUID(), "sender@acme.test");
        NotificationCreateRequest request = new NotificationCreateRequest(
                recipientId, NotificationType.TASK_ASSIGNED, "Nope", "Blocked",
                "task", UUID.randomUUID());

        assertThatThrownBy(() -> service.create(caller, request, "wrong-token"))
                .hasMessageContaining("Trusted internal service credentials");
    }

    @Test
    void get_forForeignNotification_throwsNotFound() {
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(UUID.randomUUID(), NotificationType.SYSTEM,
                "Someone else", null, null, null);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        CurrentUser caller = new CurrentUser(UUID.randomUUID(), "caller@acme.test");

        assertThatThrownBy(() -> service.get(notificationId, caller))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void markRead_forOwner_setsReadStatusAndTimestamp() {
        UUID ownerId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = new Notification(ownerId, NotificationType.TASK_SUBMITTED,
                "Submission", null, null, null);
        notification.setStatus(NotificationStatus.DELIVERED);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
        CurrentUser caller = new CurrentUser(ownerId, "owner@acme.test");

        NotificationResponse response = service.markRead(notificationId, caller);

        assertThat(response.status()).isEqualTo(NotificationStatus.READ);
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void unreadCount_returnsRepositoryCount() {
        CurrentUser caller = new CurrentUser(UUID.randomUUID(), "owner@acme.test");
        when(notificationRepository.countByUserIdAndStatus(caller.userId(), NotificationStatus.DELIVERED))
                .thenReturn(7L);

        UnreadCountResponse response = service.unreadCount(caller);

        assertThat(response.unread()).isEqualTo(7L);
    }
}
