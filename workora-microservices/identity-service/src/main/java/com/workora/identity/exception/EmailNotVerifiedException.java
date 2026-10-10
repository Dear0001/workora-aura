package com.workora.identity.exception;

public class EmailNotVerifiedException extends AppException {
    public EmailNotVerifiedException() {
        super("Verify your email address before signing in.");
    }
}
