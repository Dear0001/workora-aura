package com.workora.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ApproveFeeRequest(
        @NotNull(message = "Amount is required")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency,

        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note
) {
}
