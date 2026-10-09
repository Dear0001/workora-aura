package com.workora.project.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class ProjectEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public ProjectEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(String eventType, UUID projectId, UUID companyId, UUID actorUserId, UUID targetUserId, Map<String, Object> payload) {
        applicationEventPublisher.publishEvent(new ProjectDomainEvent(
                eventType,
                projectId,
                companyId,
                actorUserId,
                targetUserId,
                payload == null ? Map.of() : payload,
                Instant.now()
        ));
    }
}
