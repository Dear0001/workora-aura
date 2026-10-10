package com.workora.identity.exception;

public class KeycloakIdentityAlreadyExistsException extends AppException {
    public KeycloakIdentityAlreadyExistsException() {
        super("A Keycloak identity already exists for this email.");
    }
}
