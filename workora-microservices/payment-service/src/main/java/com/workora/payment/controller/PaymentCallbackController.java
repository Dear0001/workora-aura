package com.workora.payment.controller;

import com.workora.common.dto.ApiResponse;
import com.workora.payment.dto.PaymentResponse;
import com.workora.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/callbacks")
public class PaymentCallbackController {
    private final PaymentService service;
    private final String callbackSecret;

    public PaymentCallbackController(PaymentService service,
                                     @Value("${workora.payment.bakong.callback-secret:}") String callbackSecret) {
        this.service = service;
        this.callbackSecret = callbackSecret;
    }

    @PostMapping("/bakong")
    public ApiResponse<PaymentResponse> callback(
            @RequestHeader(value = "X-Bakong-Callback-Secret", required = false) String receivedSecret,
            @RequestBody Map<String, Object> payload) {
        if (!callbackSecret.isBlank() && !callbackSecret.equals(receivedSecret)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid callback secret");
        }
        String reference = value(payload, "reference", "merchantReference", "orderId");
        String providerReference = value(payload, "transactionId", "transactionReference", "providerReference", "id");
        String status = value(payload, "status", "transactionStatus", "state");
        if (status == null && payload.get("success") instanceof Boolean success) {
            status = success ? "SUCCESS" : "FAILED";
        }
        if (reference == null || providerReference == null || status == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Callback must contain reference, transaction ID, and status");
        }
        boolean successful = switch (status.toUpperCase()) {
            case "SUCCESS", "SUCCEEDED", "PAID", "COMPLETED", "SUCCESSFUL" -> true;
            default -> false;
        };
        return ApiResponse.success("Callback accepted",
                service.handleCallback(reference, providerReference, successful));
    }

    private String value(Map<String, Object> payload, String... names) {
        for (String name : names) {
            Object value = payload.get(name);
            if (value != null && !value.toString().isBlank()) return value.toString();
        }
        return null;
    }
}
