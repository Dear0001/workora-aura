package com.workora.notification.service;

import com.workora.notification.entity.NotificationType;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NotificationTemplateService {

    public Template render(String eventType, Map<String, Object> event) {
        String reference = value(event, "reference", "the requested item");
        return switch (eventType) {
            case "payment.paid" -> new Template(NotificationType.PAYMENT,
                    "Payment completed",
                    "Payment " + reference + " was completed.");
            case "task.assigned" -> new Template(NotificationType.TASK_ASSIGNED,
                    "Task assigned to you",
                    "You have been assigned a new task.");
            case "task.status.changed" -> new Template(NotificationType.TASK_STATUS_CHANGED,
                    "Task status changed",
                    "The status of your task has changed.");
            case "task.submitted" -> new Template(NotificationType.TASK_SUBMITTED,
                    "Task submission received",
                    "A task submission is ready for review.");
            case "bug-report.status.changed" -> new Template(NotificationType.BUG_REPORT_STATUS,
                    "Bug report status changed",
                    "The status of your bug report has changed.");
            case "recruitment.application.status.changed" -> new Template(NotificationType.RECRUITMENT_STATUS,
                    "Recruitment application updated",
                    "The status of your recruitment application has changed.");
            default -> new Template(NotificationType.SYSTEM,
                    "Workora notification",
                    value(event, "message", "There is an update in Workora."));
        };
    }

    private String value(Map<String, Object> event, String key, String fallback) {
        Object value = event.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    public record Template(NotificationType type, String title, String body) {
    }
}
