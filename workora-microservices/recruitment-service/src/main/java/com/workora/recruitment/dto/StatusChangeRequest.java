package com.workora.recruitment.dto;

import com.workora.recruitment.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull ApplicationStatus status,
        String reason
) {
}
