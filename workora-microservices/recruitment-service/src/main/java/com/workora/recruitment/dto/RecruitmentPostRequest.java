package com.workora.recruitment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RecruitmentPostRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String description,
        BigDecimal fee,
        String currency,
        String imageUrl,
        String intendedRole,
        UUID departmentId,
        UUID projectId
) {
}
