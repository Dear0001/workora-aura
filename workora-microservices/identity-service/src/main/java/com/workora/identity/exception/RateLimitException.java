package com.workora.identity.exception;

public class RateLimitException extends AppException {
    public RateLimitException(String message) {
        super(message);
    }
}
