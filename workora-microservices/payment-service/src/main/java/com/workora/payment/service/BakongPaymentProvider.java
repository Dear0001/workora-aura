package com.workora.payment.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "workora.payment.provider", havingValue = "bakong")
public class BakongPaymentProvider implements PaymentProvider {
    private final RestClient client;
    private final String username;
    private final String password;
    private final String staticToken;
    private final String authenticationPath;
    private final String createTransactionPath;
    private final String transactionStatusPath;
    private final String callbackUrl;
    private volatile String cachedToken;

    public BakongPaymentProvider(
            RestClient.Builder builder,
            @Value("${workora.payment.bakong.base-url}") String baseUrl,
            @Value("${workora.payment.bakong.username:}") String username,
            @Value("${workora.payment.bakong.password:}") String password,
            @Value("${workora.payment.bakong.token:}") String staticToken,
            @Value("${workora.payment.bakong.authentication-path:/authentication}") String authenticationPath,
            @Value("${workora.payment.bakong.create-transaction-path:/create-transaction}") String createTransactionPath,
            @Value("${workora.payment.bakong.transaction-status-path:/transaction-status}") String transactionStatusPath,
            @Value("${workora.payment.bakong.callback-url:}") String callbackUrl) {
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("BAKONG_BASE_URL is required when PAYMENT_PROVIDER=bakong");
        }
        this.client = builder.baseUrl(baseUrl).build();
        this.username = username;
        this.password = password;
        this.staticToken = staticToken;
        this.authenticationPath = authenticationPath;
        this.createTransactionPath = createTransactionPath;
        this.transactionStatusPath = transactionStatusPath;
        this.callbackUrl = callbackUrl;
    }

    @Override
    public String name() {
        return "bakong";
    }

    @Override
    public String createQr(String reference, BigDecimal amount, String currency, String purpose) {
        return createTransaction(reference, amount, currency, purpose, callbackUrl).qrCode();
    }

    @Override
    public boolean verify(String reference, String providerReference, BigDecimal amount, String currency) {
        return transactionStatus(reference, providerReference, amount, currency).successful();
    }

    @Override
    public PaymentCreation createTransaction(String reference, BigDecimal amount, String currency,
                                             String purpose, String callbackUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("reference", reference);
        body.put("amount", amount);
        body.put("currency", currency);
        body.put("purpose", purpose);
        body.put("callbackUrl", callbackUrl == null ? this.callbackUrl : callbackUrl);
        Map<String, Object> response = unwrap(post(createTransactionPath, body));
        String qrCode = firstString(response, "qrCode", "qr", "qrString", "qrPayload");
        String deepLink = firstString(response, "deepLink", "deeplink", "deep_link");
        String providerReference = firstString(response, "transactionId", "transactionReference",
                "providerReference", "id");
        if (qrCode == null || providerReference == null) {
            throw new IllegalStateException("Bakong create-transaction response did not contain QR code and transaction ID");
        }
        return new PaymentCreation(qrCode, deepLink, providerReference);
    }

    @Override
    public PaymentStatusResult transactionStatus(String reference, String providerReference,
                                                 BigDecimal amount, String currency) {
        Map<String, Object> body = new HashMap<>();
        body.put("reference", reference);
        body.put("transactionId", providerReference);
        body.put("amount", amount);
        body.put("currency", currency);
        Map<String, Object> response = unwrap(post(transactionStatusPath, body));
        String status = firstString(response, "status", "transactionStatus", "state");
        boolean successful = status != null && switch (status.toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "SUCCEEDED", "PAID", "COMPLETED", "SUCCESSFUL" -> true;
            default -> false;
        };
        String resolvedReference = firstString(response, "transactionId", "transactionReference",
                "providerReference", "id");
        return new PaymentStatusResult(successful,
                resolvedReference == null ? providerReference : resolvedReference);
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        String token = authenticate();
        Map<String, Object> response = client.post().uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(token))
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        if (response == null) {
            throw new IllegalStateException("Bakong returned an empty response from " + path);
        }
        return response;
    }

    private String authenticate() {
        if (!staticToken.isBlank()) {
            return staticToken;
        }
        if (cachedToken != null) {
            return cachedToken;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("BAKONG_TOKEN or BAKONG_USERNAME/BAKONG_PASSWORD is required");
        }
        synchronized (this) {
            if (cachedToken != null) {
                return cachedToken;
            }
            Map<String, Object> body = Map.of("username", username, "password", password);
            Map<String, Object> response = client.post().uri(authenticationPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            String token = response == null ? null
                    : firstString(unwrap(response), "token", "accessToken", "access_token", "jwt");
            if (token == null) {
                throw new IllegalStateException("Bakong authentication response did not contain a JWT");
            }
            cachedToken = token;
            return token;
        }
    }

    private String firstString(Map<String, Object> response, String... keys) {
        for (String key : keys) {
            Object value = response.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrap(Map<String, Object> response) {
        for (String key : new String[]{"data", "result", "response"}) {
            Object nested = response.get(key);
            if (nested instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
        }
        return response;
    }
}
