package com.workora.payment.exception;

import com.workora.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class PaymentExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiResponse<Void> notFound(PaymentNotFoundException ex) { return ApiResponse.failure(ex.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Void> invalid(IllegalArgumentException ex) { return ApiResponse.failure(ex.getMessage()); }
}
