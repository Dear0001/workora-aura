package com.workora.project.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProjectDomainEvent(
        String eventType,
        UUID projectId,
        UUID companyId,
        UUID actorUserId,
        UUID targetUserId,
        Map<String, Object> payload,
        Instant occurredAt
) {
}
