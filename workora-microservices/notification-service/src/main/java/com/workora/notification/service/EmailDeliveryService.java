package com.workora.notification.service;

import com.workora.notification.entity.*;
import com.workora.notification.repository.DeliveryAttemptRepository;
import com.workora.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.ObjectProvider;
import com.workora.notification.exception.AppException;

@Service
public class EmailDeliveryService {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final DeliveryAttemptRepository attempts;
    private final NotificationRepository notifications;
    private final boolean enabled;
    private final String from;

    public EmailDeliveryService(ObjectProvider<JavaMailSender> mailSender, DeliveryAttemptRepository attempts,
                                NotificationRepository notifications,
                                @Value("${workora.notifications.email-enabled:false}") boolean enabled,
                                @Value("${workora.notifications.from:}") String from) {
        this.mailSender = mailSender;
        this.attempts = attempts;
        this.notifications = notifications;
        this.enabled = enabled;
        this.from = from;
    }

    @Scheduled(fixedDelayString = "${workora.notifications.retry-delay-ms:60000}")
    @Transactional
    public void deliverPending() {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!enabled || sender == null) return;
        for (DeliveryAttempt attempt : attempts.findByChannelAndStatusIn(
                NotificationChannel.EMAIL, java.util.List.of("PENDING", "FAILED"))) {
            Notification notification = notifications.findById(attempt.getNotificationId()).orElse(null);
            if (notification == null || notification.getRecipientEmail() == null) continue;
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (from != null && !from.isBlank()) {
                    message.setFrom(from);
                }
                message.setTo(notification.getRecipientEmail());
                message.setSubject(notification.getTitle());
                message.setText(notification.getBody() == null ? "" : notification.getBody());
                sender.send(message);
                attempt.mark("SENT", null);
                attempts.save(attempt);
            } catch (RuntimeException ex) {
                attempt.mark("FAILED",
                        ex.getMessage() == null ? "Email delivery failed" : ex.getMessage());
                attempts.save(attempt);
            }
        }
    }

    /** Sends a diagnostic message without creating a user notification. */
    public void sendTestEmail(String recipient, String subject, String body) {
        if (recipient == null || recipient.isBlank()) {
            throw new AppException("Authenticated user has no email address");
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!enabled || sender == null) {
            throw new AppException("Email delivery is not enabled");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject == null || subject.isBlank() ? "Workora test email" : subject);
        message.setText(body == null ? "Workora email delivery is configured correctly." : body);
        sender.send(message);
    }
}
