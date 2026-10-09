package com.workora.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentActionRequest(
        @NotBlank @Size(max = 2000) String reason
) {}
