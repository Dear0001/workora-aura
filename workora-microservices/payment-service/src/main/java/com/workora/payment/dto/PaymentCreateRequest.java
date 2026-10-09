package com.workora.payment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PaymentCreateRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 3) String currency,
        @NotBlank @Size(max = 255) String purpose,
        @Size(max = 120) String idempotencyKey
) {}
