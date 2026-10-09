package com.workora.organization.dto;

import com.workora.organization.entity.Subscription;
import com.workora.organization.entity.SubscriptionPlan;

import java.time.Instant;
import java.util.UUID;

public record AdminSubscriptionResponse(
        UUID userId,
        SubscriptionPlan plan,
        int maxCompanies,
        boolean entitled,
        Instant createdAt,
        Instant updatedAt) {
    public static AdminSubscriptionResponse from(Subscription subscription) {
        return new AdminSubscriptionResponse(
                subscription.getUserId(),
                subscription.getPlan(),
                subscription.getMaxCompanies(),
                subscription.isEntitled(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }
}
