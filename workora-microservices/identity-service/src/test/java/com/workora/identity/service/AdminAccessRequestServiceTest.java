package com.workora.identity.service;

import com.workora.identity.entity.AdminAccessRequest;
import com.workora.identity.entity.AdminAccessRequestStatus;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.exception.AppException;
import com.workora.identity.repository.AdminAccessRequestRepository;
import com.workora.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccessRequestServiceTest {
    @Mock private AdminAccessRequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private KeycloakService keycloakService;
    @Mock private PasswordEncoder passwordEncoder;

    private AdminAccessRequestService service() {
        return new AdminAccessRequestService(requestRepository, userRepository, keycloakService, passwordEncoder);
    }

    @Test
    void creatingRequestProvisionsOnlyRegularAccountBeforeApproval() {
        UUID keycloakUserId = UUID.randomUUID();
        when(userRepository.findByEmail("new-admin@example.com")).thenReturn(Optional.empty());
        when(keycloakService.createAdminCandidate("new-admin@example.com", "New", "Admin"))
                .thenReturn(keycloakUserId);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(requestRepository.save(any(AdminAccessRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminAccessRequest request = service().create(" New-Admin@example.com ", "New", "Admin", "current-admin@example.com");

        verify(keycloakService).createAdminCandidate("new-admin@example.com", "New", "Admin");
        assertThat(request.getStatus()).isEqualTo(AdminAccessRequestStatus.PENDING);
        assertThat(request.getEmail()).isEqualTo("new-admin@example.com");
        verify(userRepository).save(argThat(user -> user.getRole() == Role.USER
                && !user.isEmailVerified()
                && keycloakUserId.equals(user.getKeycloakUserId())));
        verify(keycloakService, never()).setRealmAdminRole(anyString(), eq(true));
    }

    @Test
    void approvalByAnotherAdminGrantsRealmAndIdentityRoles() {
        User candidate = new User("candidate@example.com", "hash", "Candidate", "User", Role.USER);
        AdminAccessRequest request = new AdminAccessRequest("candidate@example.com", "requester@example.com");
        when(requestRepository.findById(any())).thenReturn(Optional.of(request));
        when(userRepository.findByEmail("candidate@example.com")).thenReturn(Optional.of(candidate));
        when(userRepository.saveAndFlush(candidate)).thenReturn(candidate);
        when(requestRepository.saveAndFlush(request)).thenReturn(request);

        AdminAccessRequest approved = service().approve(UUID.randomUUID(), "approver@example.com");

        verify(keycloakService).setRealmAdminRole("candidate@example.com", true);
        assertThat(candidate.getRole()).isEqualTo(Role.ADMIN);
        assertThat(approved.getStatus()).isEqualTo(AdminAccessRequestStatus.APPROVED);
        assertThat(approved.getDecidedBy()).isEqualTo("approver@example.com");
    }

    @Test
    void candidateCannotApproveOwnRequest() {
        AdminAccessRequest request = new AdminAccessRequest("admin@example.com", "other-admin@example.com");
        when(requestRepository.findById(any())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service().approve(UUID.randomUUID(), "ADMIN@example.com"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("cannot approve your own");
        verifyNoInteractions(keycloakService);
    }
}
