package com.workora.identity.service;

import com.workora.identity.entity.AdminAccessRequest;
import com.workora.identity.entity.AdminAccessRequestStatus;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.exception.AppException;
import com.workora.identity.repository.AdminAccessRequestRepository;
import com.workora.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AdminAccessRequestService {
    private final AdminAccessRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;
    private final PasswordEncoder passwordEncoder;

    public AdminAccessRequestService(
            AdminAccessRequestRepository requestRepository,
            UserRepository userRepository,
            KeycloakService keycloakService,
            PasswordEncoder passwordEncoder) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.keycloakService = keycloakService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminAccessRequest> list(AdminAccessRequestStatus status) {
        return requestRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public AdminAccessRequest create(String email, String firstName, String lastName, String requester) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        var existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isEmpty()) {
            UUID keycloakUserId = keycloakService.createAdminCandidate(normalizedEmail, firstName, lastName);
            User candidate = new User(
                    normalizedEmail,
                    passwordEncoder.encode(UUID.randomUUID().toString()),
                    firstName == null ? "" : firstName.trim(),
                    lastName == null ? "" : lastName.trim(),
                    Role.USER);
            candidate.setKeycloakUserId(keycloakUserId);
            existing = java.util.Optional.of(userRepository.save(candidate));
        }
        var candidate = existing.orElseThrow();
        if (candidate.getRole() == Role.ADMIN) {
            throw new AppException("This account already has administrator access.");
        }
        if (requestRepository.existsByEmailAndStatus(normalizedEmail, AdminAccessRequestStatus.PENDING)) {
            throw new AppException("An administrator access request is already pending for this account.");
        }
        return requestRepository.save(new AdminAccessRequest(normalizedEmail, requester));
    }

    @Transactional
    public AdminAccessRequest approve(UUID requestId, String approver) {
        AdminAccessRequest request = pendingRequest(requestId);
        if (request.getEmail().equalsIgnoreCase(approver)) {
            throw new AppException("You cannot approve your own administrator access request.");
        }
        var candidate = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException("The requested account no longer exists."));
        if (candidate.getRole() == Role.ADMIN) {
            throw new AppException("The requested account already has administrator access.");
        }

        candidate.setRole(Role.ADMIN);
        userRepository.saveAndFlush(candidate);
        request.decide(approver, AdminAccessRequestStatus.APPROVED);
        AdminAccessRequest savedRequest = requestRepository.saveAndFlush(request);
        keycloakService.setRealmAdminRole(candidate.getEmail(), true);
        return savedRequest;
    }

    @Transactional
    public AdminAccessRequest reject(UUID requestId, String approver) {
        AdminAccessRequest request = pendingRequest(requestId);
        if (request.getEmail().equalsIgnoreCase(approver)) {
            throw new AppException("You cannot reject your own administrator access request.");
        }
        request.decide(approver, AdminAccessRequestStatus.REJECTED);
        return requestRepository.save(request);
    }

    private AdminAccessRequest pendingRequest(UUID requestId) {
        AdminAccessRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException("Administrator access request not found."));
        if (request.getStatus() != AdminAccessRequestStatus.PENDING) {
            throw new AppException("This administrator access request has already been decided.");
        }
        return request;
    }
}
