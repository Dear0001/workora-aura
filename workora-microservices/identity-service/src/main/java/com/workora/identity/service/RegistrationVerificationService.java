package com.workora.identity.service;

import com.workora.identity.dto.RegistrationVerificationSessionResponse;
import com.workora.identity.entity.RegistrationVerificationSession;
import com.workora.identity.entity.User;
import com.workora.identity.exception.AppException;
import com.workora.identity.repository.RegistrationVerificationSessionRepository;
import com.workora.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RegistrationVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationVerificationSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final Duration sessionTtl;

    public RegistrationVerificationService(
            RegistrationVerificationSessionRepository sessionRepository,
            UserRepository userRepository,
            KeycloakService keycloakService,
            @Value("${registration.verification-session-ttl:PT15M}") Duration sessionTtl
    ) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.sessionTtl = sessionTtl;
    }

    @Transactional
    public String createSession(User user) {
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant now = Instant.now();
        sessionRepository.save(new RegistrationVerificationSession(
                sha256(token), user.getId(), user.getEmail(), now, now.plus(sessionTtl)));
        return token;
    }

    @Transactional
    public RegistrationVerificationSessionResponse poll(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException("Invalid registration verification session");
        }
        RegistrationVerificationSession session = sessionRepository.findByTokenHash(sha256(token))
                .orElseThrow(() -> new AppException("Invalid registration verification session"));
        Instant now = Instant.now();
        if (session.getVerifiedAt() != null) {
            return response(session, "VERIFIED");
        }
        if (!now.isBefore(session.getExpiresAt())) {
            return response(session, "EXPIRED");
        }
        if (keycloakService.isEmailVerified(session.getEmail())) {
            User user = userRepository.findById(session.getUserId())
                    .orElseThrow(() -> new AppException("Registration user profile not found"));
            user.setEmailVerified(true);
            userRepository.save(user);
            session.markVerified(now);
            sessionRepository.save(session);
            return response(session, "VERIFIED");
        }
        return response(session, "PENDING");
    }

    private RegistrationVerificationSessionResponse response(
            RegistrationVerificationSession session, String status) {
        return new RegistrationVerificationSessionResponse(status, session.getExpiresAt());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
