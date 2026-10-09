package com.workora.organization.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class OrganizationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public OrganizationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(String eventType, UUID companyId, UUID actorUserId, UUID targetUserId, Map<String, Object> payload) {
        applicationEventPublisher.publishEvent(new OrganizationDomainEvent(
                eventType,
                companyId,
                actorUserId,
                targetUserId,
                payload == null ? Map.of() : payload,
                Instant.now()
        ));
    }
}
