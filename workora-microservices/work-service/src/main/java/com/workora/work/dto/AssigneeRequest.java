package com.workora.work.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssigneeRequest(
        @NotNull UUID userId
) {
}
