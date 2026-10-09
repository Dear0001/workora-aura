package com.workora.organization.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CompanyRatingRequest(
        @Min(1) @Max(5) int score,
        @Size(max = 2000) String review
) {
}
