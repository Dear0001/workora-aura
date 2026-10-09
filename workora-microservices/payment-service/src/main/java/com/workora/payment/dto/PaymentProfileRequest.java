package com.workora.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentProfileRequest(
        @NotBlank @Size(max = 200) String walletName,
        @NotBlank @Size(max = 40) String phoneNumber,
        boolean isPrimary
) {}
