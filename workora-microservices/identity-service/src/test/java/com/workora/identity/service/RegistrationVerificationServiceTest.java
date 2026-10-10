package com.workora.identity.service;

import com.workora.identity.dto.RegistrationVerificationSessionResponse;
import com.workora.identity.entity.RegistrationVerificationSession;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.repository.RegistrationVerificationSessionRepository;
import com.workora.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationVerificationServiceTest {

    private final RegistrationVerificationSessionRepository sessionRepository =
            mock(RegistrationVerificationSessionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final KeycloakService keycloakService = mock(KeycloakService.class);
    private final RegistrationVerificationService service = new RegistrationVerificationService(
            sessionRepository, userRepository, keycloakService, java.time.Duration.ofMinutes(15));

    @Test
    void pollReturnsVerifiedWhenKeycloakVerifiedAfterSessionExpiry() {
        UUID userId = UUID.randomUUID();
        String token = "session-token";
        String tokenHash = hash(token);
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                tokenHash, userId, "user@workora.test", Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1800));
        User user = new User("user@workora.test", "encoded", "Ada", "Lovelace", Role.USER);
        UUID keycloakUserId = UUID.randomUUID();
        user.setKeycloakUserId(keycloakUserId);
        when(sessionRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(keycloakService.isEmailVerified(keycloakUserId)).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        RegistrationVerificationSessionResponse response = service.poll(token);

        assertThat(response.status()).isEqualTo("VERIFIED");
        assertThat(session.getVerifiedAt()).isNotNull();
        assertThat(user.isEmailVerified()).isTrue();
        verify(userRepository).save(user);
        verify(sessionRepository).save(session);
    }

    @Test
    void pollReturnsExpiredWhenSessionExpiredAndKeycloakStillUnverified() {
        String token = "session-token";
        String tokenHash = hash(token);
        RegistrationVerificationSession session = new RegistrationVerificationSession(
                tokenHash, UUID.randomUUID(), "user@workora.test", Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(1800));
        UUID localUserId = session.getUserId();
        User user = new User("user@workora.test", "encoded", "Ada", "Lovelace", Role.USER);
        UUID keycloakUserId = UUID.randomUUID();
        user.setKeycloakUserId(keycloakUserId);
        when(sessionRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(session));
        when(userRepository.findById(localUserId)).thenReturn(Optional.of(user));
        when(keycloakService.isEmailVerified(keycloakUserId)).thenReturn(false);

        RegistrationVerificationSessionResponse response = service.poll(token);

        assertThat(response.status()).isEqualTo("EXPIRED");
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
