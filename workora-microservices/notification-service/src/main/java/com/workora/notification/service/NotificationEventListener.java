package com.workora.notification.service;

import com.workora.notification.dto.NotificationCreateRequest;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationEventListener {
    private final NotificationService service;
    private final NotificationTemplateService templates;

    public NotificationEventListener(NotificationService service, NotificationTemplateService templates) {
        this.service = service;
        this.templates = templates;
    }
    @RabbitListener(queues = "notification.events")
    public void onEvent(Map<String, Object> event,
                        @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) {
        Object userId = event.get("userId");
        if (userId == null) return;
        String eventType = stringValue(event.get("eventType"));
        if (eventType == null || eventType.isBlank()) {
            eventType = routingKey == null ? "" : routingKey;
        }
        NotificationTemplateService.Template template = templates.render(eventType, event);
        UUID relatedId = uuid(event.get("paymentId"));
        if (relatedId == null) {
            relatedId = uuid(event.get("entityId"));
        }
        service.create(null, new NotificationCreateRequest(
                UUID.fromString(String.valueOf(userId)), template.type(),
                template.title(), template.body(),
                eventType, relatedId, stringValue(event.get("recipientEmail"))));
    }

    private UUID uuid(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
