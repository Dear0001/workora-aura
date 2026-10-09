package com.workora.organization.dto;

import com.workora.organization.entity.SubscriptionPlan;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminSubscriptionUpdateRequest(
        @NotNull SubscriptionPlan plan,
        @Min(1) @Max(10000) int maxCompanies,
        boolean entitled) {
}
