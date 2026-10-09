package com.workora.organization.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record OrganizationDomainEvent(
        String eventType,
        UUID companyId,
        UUID actorUserId,
        UUID targetUserId,
        Map<String, Object> payload,
        Instant occurredAt
) {
}
