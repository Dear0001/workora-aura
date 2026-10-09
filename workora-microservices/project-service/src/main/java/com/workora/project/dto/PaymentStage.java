package com.workora.project.dto;

/**
 * The two job-side states a future Payment service will drive. This is the deferred
 * Bakong payment seam: the job only records the stage and reference, never credentials.
 */
public enum PaymentStage {
    WAITING_FOR_COMPANY_PAYMENT,
    PAYMENT_VERIFIED
}
