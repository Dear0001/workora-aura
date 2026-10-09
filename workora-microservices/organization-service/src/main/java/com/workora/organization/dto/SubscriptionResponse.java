package com.workora.organization.dto;

import com.workora.organization.entity.Subscription;
import com.workora.organization.entity.SubscriptionPlan;

import java.util.UUID;

public record SubscriptionResponse(
        UUID userId,
        SubscriptionPlan plan,
        int maxCompanies,
        boolean entitled) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(subscription.getUserId(), subscription.getPlan(),
                subscription.getMaxCompanies(), subscription.isEntitled());
    }
}
