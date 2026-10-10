package com.workora.identity.service;

import com.workora.identity.dto.*;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.exception.AppException;
import com.workora.identity.exception.EmailNotVerifiedException;
import com.workora.identity.exception.KeycloakIdentityAlreadyExistsException;
import com.workora.identity.exception.RateLimitException;
import com.workora.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Duration VERIFICATION_EMAIL_RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakService keycloakService;
    private final RegistrationVerificationService registrationVerificationService;
    private final JwtDecoder jwtDecoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            KeycloakService keycloakService,
            RegistrationVerificationService registrationVerificationService,
            JwtDecoder jwtDecoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakService = keycloakService;
        this.registrationVerificationService = registrationVerificationService;
        this.jwtDecoder = jwtDecoder;
    }

    @Transactional
    public Map<String, String> register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AppException("Email already registered");
        }

        User user;
        try {
            UUID keycloakUserId = keycloakService.register(request);
            user = new User(
                    email,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    request.firstName(),
                    request.lastName(),
                    Role.USER
            );
            user.setKeycloakUserId(keycloakUserId);
        } catch (KeycloakIdentityAlreadyExistsException duplicateIdentity) {
            Map<String, Object> tokens = keycloakService.login(new LoginRequest(email, request.password()));
            KeycloakIdentity identity = identityFromTokens(tokens);
            if (!email.equals(identity.email())) {
                throw new AppException("Keycloak returned an identity that does not match the registration email.");
            }
            boolean verified = identity.emailVerified() || keycloakService.isEmailVerified(identity.userId());
            user = new User(
                    email,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    identity.firstName() == null ? request.firstName() : identity.firstName(),
                    identity.lastName() == null ? request.lastName() : identity.lastName(),
                    Role.USER
            );
            user.setKeycloakUserId(identity.userId());
            user.setEmailVerified(verified);
            if (!verified) {
                keycloakService.sendVerificationEmail(identity.userId());
            }
        }

        if (!user.isEmailVerified()) {
            user.setLastVerificationEmailSentAt(Instant.now());
        }
        User savedUser = userRepository.save(user);
        String verificationSessionToken = registrationVerificationService.createSession(savedUser);
        return registrationResponse(savedUser, verificationSessionToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Map<String, Object> tokens = keycloakService.login(new LoginRequest(email, request.password()));
        KeycloakIdentity identity = verifiedIdentity(tokens);
        if (!email.equals(identity.email())) {
            throw new AppException("Keycloak returned an identity that does not match the login request.");
        }
        return authResponse(tokens, profileFor(identity));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Map<String, Object> tokens = keycloakService.refresh(request);
        KeycloakIdentity identity = verifiedIdentity(tokens);
        return authResponse(tokens, profileFor(identity));
    }

    public String logout(LogoutRequest request) {
        keycloakService.logout(request.refreshToken());
        return "Logged out successfully";
    }

    public String requestPasswordReset(PasswordResetRequest request) {
        keycloakService.requestPasswordReset(normalizeEmail(request.email()));
        return "A password reset email will be sent if the account exists.";
    }

    @Transactional
    public String requestVerificationEmail(PasswordResetRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailForUpdate(email).ifPresent(user -> {
            Instant now = Instant.now();
            if (user.isEmailVerified() || keycloakService.isEmailVerifiedByEmail(email)) {
                user.setEmailVerified(true);
                userRepository.save(user);
                return;
            }

            Instant lastSentAt = user.getLastVerificationEmailSentAt();
            if (lastSentAt != null && now.isBefore(lastSentAt.plus(VERIFICATION_EMAIL_RESEND_COOLDOWN))) {
                throw new RateLimitException("Please wait before requesting another verification email.");
            }

            keycloakService.resendVerificationEmail(email);
            user.setLastVerificationEmailSentAt(now);
            userRepository.save(user);
        });
        return "A verification email will be sent if the account exists.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        throw new AppException("Password reset is completed through the Keycloak email link.");
    }

    private User profileFor(KeycloakIdentity identity) {
        User user = userRepository.findByKeycloakUserId(identity.userId()).orElse(null);
        if (user == null) {
            user = userRepository.findByEmail(identity.email()).orElse(null);
            if (user != null && user.getKeycloakUserId() != null
                    && !user.getKeycloakUserId().equals(identity.userId())) {
                throw new AppException("This profile is already linked to another identity.");
            }
        }

        if (user == null) {
            user = new User(
                    identity.email(),
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    identity.firstName(),
                    identity.lastName(),
                    Role.USER
            );
        } else {
            if (!user.getEmail().equals(identity.email())) {
                User conflictingProfile = userRepository.findByEmail(identity.email()).orElse(null);
                if (conflictingProfile != null && !conflictingProfile.getId().equals(user.getId())) {
                    throw new AppException("The verified identity email conflicts with another profile.");
                }
                user.setEmail(identity.email());
            }
            if ((user.getFirstName() == null || user.getFirstName().isBlank())
                    && identity.firstName() != null) {
                user.setFirstName(identity.firstName());
            }
            if ((user.getLastName() == null || user.getLastName().isBlank())
                    && identity.lastName() != null) {
                user.setLastName(identity.lastName());
            }
        }

        user.setKeycloakUserId(identity.userId());
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private KeycloakIdentity verifiedIdentity(Map<String, Object> tokens) {
        KeycloakIdentity identity = identityFromTokens(tokens);
        if (!identity.emailVerified() && !keycloakService.isEmailVerified(identity.userId())) {
            String refreshToken = (String) tokens.get("refresh_token");
            try {
                keycloakService.logout(refreshToken);
            } catch (RuntimeException exception) {
                org.slf4j.LoggerFactory.getLogger(AuthService.class)
                        .warn("Could not revoke tokens returned for an unverified Keycloak identity", exception);
            }
            throw new EmailNotVerifiedException();
        }
        return identity;
    }

    private KeycloakIdentity identityFromTokens(Map<String, Object> tokens) {
        String accessToken = (String) tokens.get("access_token");
        String refreshToken = (String) tokens.get("refresh_token");
        if (accessToken == null || refreshToken == null) {
            throw new AppException("Keycloak did not return a complete token response");
        }

        Jwt jwt = jwtDecoder.decode(accessToken);
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AppException("Keycloak returned an invalid user identifier.");
        }
        String email = normalizeEmail(jwt.getClaimAsString("email"));
        if (email == null || email.isBlank()) {
            throw new AppException("Keycloak token does not contain an email claim");
        }

        return new KeycloakIdentity(
                userId,
                email,
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                Boolean.TRUE.equals(jwt.getClaim("email_verified"))
        );
    }

    private Map<String, String> registrationResponse(User user, String verificationSessionToken) {
        return Map.of(
                "userId", user.getKeycloakUserId().toString(),
                "email", user.getEmail(),
                "verificationSessionToken", verificationSessionToken,
                "message", "Registration completed. Check your email or continue to sign in if already verified."
        );
    }

    private AuthResponse authResponse(Map<String, Object> tokens, User user) {
        String accessToken = (String) tokens.get("access_token");
        String refreshToken = (String) tokens.get("refresh_token");
        if (accessToken == null || refreshToken == null) {
            throw new AppException("Keycloak did not return a complete token response");
        }
        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getKeycloakUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isEmailVerified()
        );
    }

    @Transactional
    public UserProfileResponse getCurrentProfile(UUID keycloakUserId, String email) {
        if (!keycloakService.isEmailVerified(keycloakUserId)) {
            throw new EmailNotVerifiedException();
        }
        return toProfileResponse(profileFor(new KeycloakIdentity(
                keycloakUserId,
                normalizeEmail(email),
                null,
                null,
                true
        )));
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(
            UUID keycloakUserId,
            String email,
            UpdateProfileCommand command
    ) {
        if (!keycloakService.isEmailVerified(keycloakUserId)) {
            throw new EmailNotVerifiedException();
        }
        User user = profileFor(new KeycloakIdentity(keycloakUserId, normalizeEmail(email), null, null, true));
        if (command.firstName() != null && !command.firstName().isBlank()) {
            user.setFirstName(command.firstName().trim());
        }
        if (command.lastName() != null && !command.lastName().isBlank()) {
            user.setLastName(command.lastName().trim());
        }
        if (command.gender() != null) {
            user.setGender(command.gender());
        }
        if (command.dateOfBirth() != null) {
            user.setDateOfBirth(command.dateOfBirth());
        }
        if (command.avatarUrl() != null) {
            user.setAvatarUrl(command.avatarUrl());
        }
        if (command.bio() != null) {
            user.setBio(command.bio());
        }
        if (command.experience() != null) {
            user.setExperience(command.experience());
        }
        userRepository.save(user);
        return toProfileResponse(user);
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getKeycloakUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getExperience(),
                user.getRole().name(),
                user.isEmailVerified(),
                user.getAccountStatus()
        );
    }

    /** Optional-field profile update; a null field means "leave unchanged" (PATCH). */
    public record UpdateProfileCommand(
            String firstName,
            String lastName,
            String gender,
            java.time.LocalDate dateOfBirth,
            String avatarUrl,
            String bio,
            String experience
    ) {
    }

    private record KeycloakIdentity(
            UUID userId,
            String email,
            String firstName,
            String lastName,
            boolean emailVerified
    ) {
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
