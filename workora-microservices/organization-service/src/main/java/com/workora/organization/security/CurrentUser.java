package com.workora.organization.security;

import com.workora.organization.exception.AppException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Canonical caller identity resolved from the Keycloak-issued access token.
 * The token {@code sub} claim is the immutable cross-service user id.
 */
public record CurrentUser(UUID userId, String email) {

    public static CurrentUser from(Jwt jwt) {
        if (jwt == null) {
            throw new AppException("Authentication required");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AppException("Token is missing a subject claim");
        }
        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new AppException("Token subject is not a valid user id");
        }
        String email = jwt.getClaimAsString("email");
        return new CurrentUser(userId, email == null ? "" : email.trim().toLowerCase());
    }
}
