package com.workora.payment.dto;

import com.workora.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, String reference, BigDecimal amount, String currency,
                              String purpose, PaymentStatus status, String provider,
                              String qrPayload, String deepLink, Instant expiresAt, Instant paidAt,
                              Instant createdAt, Instant receiptConfirmedAt,
                              String disputeReason) {}
