package com.workora.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProjectAssetRequest(
        @NotNull UUID projectId,
        UUID phaseId,
        @NotBlank String name,
        @NotBlank String storageKey,
        @NotBlank String contentType,
        @NotBlank String kind
) {
}
