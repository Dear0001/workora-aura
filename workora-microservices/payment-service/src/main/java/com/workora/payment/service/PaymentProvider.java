package com.workora.payment.service;

import java.math.BigDecimal;

public interface PaymentProvider {
    String name();
    String createQr(String reference, BigDecimal amount, String currency, String purpose);
    boolean verify(String reference, String providerReference, BigDecimal amount, String currency);

    default PaymentCreation createTransaction(String reference, BigDecimal amount, String currency,
                                              String purpose, String callbackUrl) {
        return new PaymentCreation(createQr(reference, amount, currency, purpose), null, reference);
    }

    default PaymentStatusResult transactionStatus(String reference, String providerReference,
                                                  BigDecimal amount, String currency) {
        return new PaymentStatusResult(verify(reference, providerReference, amount, currency), providerReference);
    }

    record PaymentCreation(String qrCode, String deepLink, String providerReference) {}
    record PaymentStatusResult(boolean successful, String providerReference) {}
}
