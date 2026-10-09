package com.workora.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentVerifyRequest(@NotBlank String providerReference) {}
