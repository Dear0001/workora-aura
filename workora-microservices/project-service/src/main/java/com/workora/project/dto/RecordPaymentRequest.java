package com.workora.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordPaymentRequest(
        @NotNull(message = "Payment stage is required")
        PaymentStage stage,

        UUID paymentId,

        @Size(max = 200, message = "Payment reference must be at most 200 characters")
        String paymentReference,

        BigDecimal amount,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
        String currency
) {
}
