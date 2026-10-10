package com.workora.identity.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void rateLimitExceptionReturnsTooManyRequests() {
        var response = new GlobalExceptionHandler()
                .handleAppException(new RateLimitException("Please wait before requesting another verification email."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
