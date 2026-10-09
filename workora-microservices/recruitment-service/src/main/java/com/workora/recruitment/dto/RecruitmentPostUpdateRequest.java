package com.workora.recruitment.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecruitmentPostUpdateRequest(
        @Size(max = 200) String title,
        String description,
        BigDecimal fee,
        String currency,
        String imageUrl,
        String intendedRole
) {
}
