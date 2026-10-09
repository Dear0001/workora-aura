package com.workora.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "workora.payment.provider", havingValue = "local", matchIfMissing = true)
public class LocalPaymentProvider implements PaymentProvider {
    private final String provider;
    public LocalPaymentProvider(@Value("${workora.payment.provider:local}") String provider) {
        this.provider = provider;
    }
    @Override public String name() { return provider; }
    @Override public String createQr(String reference, BigDecimal amount, String currency, String purpose) {
        return "KHQR://workora?reference=" + reference + "&amount=" + amount.toPlainString()
                + "&currency=" + currency + "&purpose=" + java.net.URLEncoder.encode(
                purpose, java.nio.charset.StandardCharsets.UTF_8);
    }
    @Override public boolean verify(String reference, String providerReference, BigDecimal amount, String currency) {
        return providerReference.startsWith("SIMULATED-") || providerReference.equals(reference);
    }
}
