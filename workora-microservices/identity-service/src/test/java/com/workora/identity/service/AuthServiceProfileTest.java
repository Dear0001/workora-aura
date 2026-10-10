package com.workora.identity.service;

import com.workora.identity.dto.AuthResponse;
import com.workora.identity.dto.LoginRequest;
import com.workora.identity.dto.PasswordResetRequest;
import com.workora.identity.dto.RegisterRequest;
import com.workora.identity.entity.AccountStatus;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.exception.EmailNotVerifiedException;
import com.workora.identity.exception.KeycloakIdentityAlreadyExistsException;
import com.workora.identity.exception.RateLimitException;
import com.workora.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceProfileTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private KeycloakService keycloakService;
    @Mock private RegistrationVerificationService registrationVerificationService;
    @Mock private JwtDecoder jwtDecoder;

    private AuthService authService;
    private final String email = "user@workora.test";
    private final UUID keycloakUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, keycloakService,
                registrationVerificationService, jwtDecoder);
    }

    private User existingUser() {
        User user = new User(email, "encoded", "Ada", "Lovelace", Role.USER);
        user.setKeycloakUserId(keycloakUserId);
        user.setEmailVerified(true);
        user.setBio("original bio");
        user.setGender("female");
        return user;
    }

    @Test
    void updateProfile_appliesOnlyProvidedFields() {
        User user = existingUser();
        when(keycloakService.isEmailVerified(keycloakUserId)).thenReturn(true);
        when(userRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.UpdateProfileCommand command = new AuthService.UpdateProfileCommand(
                null, null, null, LocalDate.of(1990, 1, 2), null, "new bio", null);

        var response = authService.updateCurrentProfile(keycloakUserId, email, command);

        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.gender()).isEqualTo("female");
        assertThat(response.bio()).isEqualTo("new bio");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void getCurrentProfileReturnsExpandedFieldsAndCanonicalIdentity() {
        User user = existingUser();
        user.setExperience("10 years");
        when(keycloakService.isEmailVerified(keycloakUserId)).thenReturn(true);
        when(userRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.getCurrentProfile(keycloakUserId, email);

        assertThat(response.id()).isEqualTo(keycloakUserId);
        assertThat(response.experience()).isEqualTo("10 years");
        assertThat(response.emailVerified()).isTrue();
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void loginLinksMatchingLegacyProfileToVerifiedKeycloakSubject() {
        User user = new User(email, "encoded", "Ada", "Lovelace", Role.USER);
        when(keycloakService.login(any(LoginRequest.class)))
                .thenReturn(Map.of("access_token", "access", "refresh_token", "refresh"));
        when(jwtDecoder.decode("access")).thenReturn(jwt(true));
        when(userRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest(email, "password"));

        assertThat(user.getKeycloakUserId()).isEqualTo(keycloakUserId);
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(response.userId()).isEqualTo(keycloakUserId);
        assertThat(response.emailVerified()).isTrue();
        verify(keycloakService, never()).isEmailVerified(any(UUID.class));
    }

    @Test
    void registrationRetryRepairsMissingProfileAfterKeycloakAccountWasCreated() {
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(keycloakService.register(any(RegisterRequest.class)))
                .thenThrow(new KeycloakIdentityAlreadyExistsException());
        when(keycloakService.login(any(LoginRequest.class)))
                .thenReturn(Map.of("access_token", "access", "refresh_token", "refresh"));
        when(jwtDecoder.decode("access")).thenReturn(jwt(false));
        when(keycloakService.isEmailVerified(keycloakUserId)).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("random-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(registrationVerificationService.createSession(any(User.class))).thenReturn("session-token");

        Map<String, String> response = authService.register(
                new RegisterRequest(email, "submitted-password", "Ada", "Lovelace"));

        assertThat(response.get("userId")).isEqualTo(keycloakUserId.toString());
        assertThat(response.get("verificationSessionToken")).isEqualTo("session-token");
        verify(keycloakService).sendVerificationEmail(keycloakUserId);
        verify(userRepository).save(argThat(user -> keycloakUserId.equals(user.getKeycloakUserId())
                && !user.isEmailVerified()));
    }

    @Test
    void loginRejectsUnverifiedIdentityAndRevokesRefreshToken() {
        when(keycloakService.login(any(LoginRequest.class)))
                .thenReturn(Map.of("access_token", "access", "refresh_token", "refresh"));
        when(jwtDecoder.decode("access")).thenReturn(jwt(false));
        when(keycloakService.isEmailVerified(keycloakUserId)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "password")))
                .isInstanceOf(EmailNotVerifiedException.class);
        verify(keycloakService).logout("refresh");
        verifyNoInteractions(userRepository);
    }

    @Test
    void loginRejectsEmailConflictWithAlreadyLinkedProfile() {
        User user = new User(email, "encoded", "Ada", "Lovelace", Role.USER);
        user.setKeycloakUserId(UUID.randomUUID());
        when(keycloakService.login(any(LoginRequest.class)))
                .thenReturn(Map.of("access_token", "access", "refresh_token", "refresh"));
        when(jwtDecoder.decode("access")).thenReturn(jwt(true));
        when(userRepository.findByKeycloakUserId(keycloakUserId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(email, "password")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already linked");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void verificationEmailRequestUsesGenericResponse() {
        User user = existingUser();
        user.setEmailVerified(false);
        when(userRepository.findByEmailForUpdate(email)).thenReturn(Optional.of(user));
        when(keycloakService.isEmailVerifiedByEmail(email)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String message = authService.requestVerificationEmail(new PasswordResetRequest(email));

        assertThat(message).isEqualTo("A verification email will be sent if the account exists.");
        verify(keycloakService).resendVerificationEmail(email);
        assertThat(user.getLastVerificationEmailSentAt()).isNotNull();
    }

    @Test
    void verificationEmailRequestRejectsResendDuringCooldown() {
        User user = existingUser();
        user.setEmailVerified(false);
        user.setLastVerificationEmailSentAt(Instant.now());
        when(userRepository.findByEmailForUpdate(email)).thenReturn(Optional.of(user));
        when(keycloakService.isEmailVerifiedByEmail(email)).thenReturn(false);

        assertThatThrownBy(() -> authService.requestVerificationEmail(new PasswordResetRequest(email)))
                .isInstanceOf(RateLimitException.class)
                .hasMessage("Please wait before requesting another verification email.");
        verify(keycloakService, never()).resendVerificationEmail(email);
    }

    @Test
    void verificationEmailRequestDoesNotResendForVerifiedAccount() {
        User user = existingUser();
        user.setEmailVerified(false);
        when(userRepository.findByEmailForUpdate(email)).thenReturn(Optional.of(user));
        when(keycloakService.isEmailVerifiedByEmail(email)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.requestVerificationEmail(new PasswordResetRequest(email));

        assertThat(user.isEmailVerified()).isTrue();
        verify(keycloakService, never()).resendVerificationEmail(email);
    }

    @Test
    void verificationEmailRequestForUnknownAccountKeepsGenericResponse() {
        when(userRepository.findByEmailForUpdate(email)).thenReturn(Optional.empty());

        String message = authService.requestVerificationEmail(new PasswordResetRequest(email));

        assertThat(message).isEqualTo("A verification email will be sent if the account exists.");
        verify(keycloakService, never()).resendVerificationEmail(email);
    }

    private Jwt jwt(boolean emailVerified) {
        return Jwt.withTokenValue("access")
                .header("alg", "none")
                .subject(keycloakUserId.toString())
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("given_name", "Ada")
                .claim("family_name", "Lovelace")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }
}
