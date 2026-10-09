package com.workora.identity.service;

import com.workora.identity.dto.*;
import com.workora.identity.entity.AccountStatus;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceProfileTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private KeycloakService keycloakService;
    @Mock
    private RegistrationVerificationService registrationVerificationService;

    private AuthService authService;

    private final String email = "user@workora.test";

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, keycloakService,
                registrationVerificationService);
    }

    private User existingUser() {
        User user = new User(email, "encoded", "Ada", "Lovelace", Role.USER);
        user.setEmailVerified(true);
        user.setBio("original bio");
        user.setGender("female");
        return user;
    }

    @Test
    void updateProfile_appliesOnlyProvidedFields() {
        User user = existingUser();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.UpdateProfileCommand command = new AuthService.UpdateProfileCommand(
                null, null, null, LocalDate.of(1990, 1, 2), null, "new bio", null);

        UserProfileResponse response = authService.updateCurrentProfile(email, command);

        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.lastName()).isEqualTo("Lovelace");
        assertThat(response.gender()).isEqualTo("female");
        assertThat(response.bio()).isEqualTo("new bio");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void getCurrentProfile_returnsExpandedFields() {
        User user = existingUser();
        user.setExperience("10 years");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getCurrentProfile(email);

        assertThat(response.experience()).isEqualTo("10 years");
        assertThat(response.emailVerified()).isTrue();
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void login_synchronizesEmailVerificationFromKeycloak() {
        User user = existingUser();
        user.setEmailVerified(false);
        when(keycloakService.login(any(LoginRequest.class)))
                .thenReturn(Map.of("access_token", "access", "refresh_token", "refresh"));
        when(keycloakService.isEmailVerified(email)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(new LoginRequest(email, "password"));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(response.email()).isEqualTo(email);
        verify(keycloakService).isEmailVerified(email);
    }

    @Test
    void verificationEmailRequestUsesGenericResponse() {
        String message = authService.requestVerificationEmail(new PasswordResetRequest(email));

        assertThat(message).isEqualTo("A verification email will be sent if the account exists.");
        verify(keycloakService).resendVerificationEmail(email);
    }
}
