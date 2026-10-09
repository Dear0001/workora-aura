package com.workora.payment.dto;

import java.time.Instant;
import java.util.UUID;

public record PaymentProfileResponse(UUID id, String provider, String walletName, String phoneNumber,
                                     boolean isPrimary, String qrPayload, Instant createdAt, Instant updatedAt) {}
