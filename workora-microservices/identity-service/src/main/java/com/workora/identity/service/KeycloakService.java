package com.workora.identity.service;

import com.workora.identity.dto.LoginRequest;
import com.workora.identity.dto.RefreshTokenRequest;
import com.workora.identity.dto.RegisterRequest;
import com.workora.identity.exception.AppException;
import com.workora.identity.exception.KeycloakIdentityAlreadyExistsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KeycloakService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakService.class);

    private final RestClient client;
    private final String realm;
    private final String clientId;
    private final String frontendUrl;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakService(
            RestClient.Builder builder,
            @Value("${keycloak.base-url}") String baseUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.frontend-url}") String frontendUrl,
            @Value("${keycloak.admin-username}") String adminUsername,
            @Value("${keycloak.admin-password}") String adminPassword
    ) {
        this.client = builder.baseUrl(baseUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.frontendUrl = frontendUrl;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public UUID register(RegisterRequest request) {
        final String adminToken = adminToken();
        URI location = null;
        try {
            var created = client.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .headers(headers -> headers.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "username", request.email().trim().toLowerCase(),
                            "email", request.email().trim().toLowerCase(),
                            "firstName", request.firstName() == null ? "" : request.firstName(),
                            "lastName", request.lastName() == null ? "" : request.lastName(),
                            "enabled", true,
                            "emailVerified", false,
                            "realmRoles", new String[]{"USER"},
                            "requiredActions", new String[]{"VERIFY_EMAIL"},
                            "credentials", new Object[]{
                                    Map.of("type", "password", "value", request.password(), "temporary", false)
                            }
                    ))
                    .retrieve()
                    .toBodilessEntity();
            location = created.getHeaders().getLocation();
            UUID keycloakUserId = userIdFromLocation(location);
            sendVerificationEmail(keycloakUserId, adminToken);
            return keycloakUserId;
        } catch (RestClientResponseException ex) {
            deleteCreatedUser(location, adminToken);
            String response = ex.getResponseBodyAsString().toLowerCase();
            if (ex.getStatusCode().value() == 409
                    && (response.contains("same email") || response.contains("same username"))) {
                throw new KeycloakIdentityAlreadyExistsException();
            }
            throw new AppException(keycloakError("registration or verification email", ex));
        } catch (RestClientException ex) {
            deleteCreatedUser(location, adminToken);
            log.error("Keycloak is unavailable during registration", ex);
            throw new AppException("Keycloak is unavailable. Start Keycloak and verify KEYCLOAK_BASE_URL.");
        }
    }

    private UUID userIdFromLocation(URI location) {
        if (location == null) {
            throw new AppException("Keycloak did not return the created account identifier.");
        }
        String path = location.getPath();
        String id = path.substring(path.lastIndexOf('/') + 1);
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            throw new AppException("Keycloak returned an invalid account identifier.");
        }
    }

    private void deleteCreatedUser(URI location, String adminToken) {
        if (location == null) {
            return;
        }
        String userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
        try {
            client.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .headers(headers -> headers.setBearerAuth(adminToken))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException cleanupError) {
            log.warn("Could not clean up partially created Keycloak user {}", userId, cleanupError);
        }
    }

    private String keycloakError(String operation, RestClientResponseException ex) {
        log.error("Keycloak {} failed with status {}", operation, ex.getStatusCode(), ex);
        if (ex.getStatusCode().value() == 409
                && ex.getResponseBodyAsString().contains("same email")) {
            return "Email already registered. Please sign in or use a different email address.";
        }
        return "Keycloak failed during " + operation + " (HTTP " + ex.getStatusCode().value()
                + "). Check Keycloak logs and realm SMTP settings.";
    }

    public Map<String, Object> login(LoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", request.email());
        form.add("password", request.password());
        form.add("scope", "openid profile email");
        return tokenRequest(form);
    }

    public Map<String, Object> refresh(RefreshTokenRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", request.refreshToken());
        return tokenRequest(form);
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);
        client.post()
                .uri("/realms/{realm}/protocol/openid-connect/logout", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    public void setRealmAdminRole(String email, boolean enabled) {
        String token = adminToken();
        Map<String, Object> user = findUser(email, token);
        if (user == null || user.get("id") == null) {
            throw new AppException("Keycloak account was not found for the approved admin request.");
        }
        Map<String, Object> role = client.get()
                .uri("/admin/realms/{realm}/roles/ADMIN", realm)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(Map.class);
        if (role == null || role.get("id") == null) {
            throw new AppException("Keycloak realm ADMIN role is not configured.");
        }
        var request = client.method(enabled ? org.springframework.http.HttpMethod.POST : org.springframework.http.HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, user.get("id"))
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role));
        request.retrieve().toBodilessEntity();
    }

    public UUID createAdminCandidate(String email, String firstName, String lastName) {
        String token = adminToken();
        URI location = null;
        try {
            var created = client.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "username", email,
                            "email", email,
                            "firstName", firstName == null ? "" : firstName,
                            "lastName", lastName == null ? "" : lastName,
                            "enabled", true,
                            "emailVerified", false,
                            "realmRoles", new String[]{"USER"},
                            "requiredActions", new String[]{"VERIFY_EMAIL", "UPDATE_PASSWORD"}))
                    .retrieve()
                    .toBodilessEntity();
            location = created.getHeaders().getLocation();
            UUID userId = userIdFromLocation(location);
            client.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                            .queryParam("client_id", clientId)
                            .queryParam("redirect_uri", frontendUrl + "/login")
                            .build(realm, userId))
                    .headers(headers -> headers.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new String[]{"VERIFY_EMAIL", "UPDATE_PASSWORD"})
                    .retrieve()
                    .toBodilessEntity();
            return userId;
        } catch (RestClientResponseException exception) {
            deleteCreatedUser(location, token);
            throw new AppException(keycloakError("admin account invitation", exception));
        } catch (RestClientException exception) {
            deleteCreatedUser(location, token);
            log.error("Keycloak is unavailable while creating an admin candidate", exception);
            throw new AppException("Keycloak is unavailable. The admin account was not created.");
        }
    }

    public void requestPasswordReset(String email) {
        sendRequiredActionIfPresent(email, "UPDATE_PASSWORD", adminToken(), "/reset-password?status=success");
    }

    public void resendVerificationEmail(String email) {
        String token = adminToken();
        Map<String, Object> user = findUser(email, token);
        if (user == null) {
            // Deliberately do nothing: callers must not be able to discover
            // whether an email is registered.
            return;
        }
        if (Boolean.TRUE.equals(user.get("emailVerified"))) {
            return;
        }
        sendVerificationEmail(UUID.fromString((String) user.get("id")), token);
    }

    public void sendVerificationEmail(UUID userId) {
        sendVerificationEmail(userId, adminToken());
    }

    public boolean isEmailVerified(UUID userId) {
        Map<String, Object> user = findUser(userId);
        return user != null && Boolean.TRUE.equals(user.get("emailVerified"));
    }

    public boolean isEmailVerifiedByEmail(String email) {
        Map<String, Object> user = findUser(email, adminToken());
        return user != null && Boolean.TRUE.equals(user.get("emailVerified"));
    }

    private Map<String, Object> findUser(UUID userId) {
        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users/{id}")
                        .build(realm, userId))
                .headers(headers -> headers.setBearerAuth(adminToken()))
                .retrieve()
                .body(Map.class);
    }

    private void sendVerificationEmail(UUID userId, String adminToken) {
        client.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users/{id}/send-verify-email")
                        .queryParam("client_id", clientId)
                        .queryParam("redirect_uri", frontendUrl.replaceAll("/+$", "") + "/?status=verified")
                        .build(realm, userId))
                .headers(headers -> headers.setBearerAuth(adminToken))
                .retrieve()
                .toBodilessEntity();
    }

    private void sendRequiredActionIfPresent(String email, String action, String adminToken, String redirectPath) {
        Map<String, Object> user = findUser(email, adminToken);
        if (user == null) {
            return;
        }

        client.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                        .queryParam("client_id", clientId)
                        .queryParam("redirect_uri", frontendUrl + redirectPath)
                        .build(realm, user.get("id")))
                .headers(headers -> headers.setBearerAuth(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new String[]{action})
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> findUser(String email, String adminToken) {
        Map<String, Object>[] users = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("email", email.trim().toLowerCase())
                        .queryParam("exact", true)
                        .build(realm))
                .headers(headers -> headers.setBearerAuth(adminToken))
                .retrieve()
                .body(Map[].class);
        if (users == null) return null;
        for (Map<String, Object> user : users) {
            Object candidateEmail = user.get("email");
            if (candidateEmail instanceof String value && value.equalsIgnoreCase(email.trim())) {
                return user;
            }
        }
        return null;
    }

    private Map<String, Object> tokenRequest(MultiValueMap<String, String> form) {
        return client.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
    }

    private String adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);
        Map<String, Object> response = client.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        String token = response == null ? null : (String) response.get("access_token");
        if (token == null || token.isBlank()) {
            throw new AppException("Keycloak admin authentication failed. Check KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD.");
        }
        return token;
    }
}
