package com.workora.organization.service;

import com.workora.organization.dto.InvitationRequest;
import com.workora.organization.dto.InvitationResponse;
import com.workora.organization.dto.PendingInvitationResponse;
import com.workora.organization.entity.CompanyRole;
import com.workora.organization.entity.Invitation;
import com.workora.organization.entity.InvitationStatus;
import com.workora.organization.event.OrganizationEventPublisher;
import com.workora.organization.exception.AppException;
import com.workora.organization.exception.ConflictException;
import com.workora.organization.exception.ForbiddenException;
import com.workora.organization.exception.NotFoundException;
import com.workora.organization.mapper.OrganizationMapper;
import com.workora.organization.repository.InvitationRepository;
import com.workora.organization.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InvitationService {

    private static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final MembershipService membershipService;
    private final CompanyService companyService;
    private final DepartmentService departmentService;
    private final OrganizationMapper mapper;
    private final OrganizationEventPublisher organizationEventPublisher;

    public InvitationService(InvitationRepository invitationRepository,
                             MembershipService membershipService,
                             CompanyService companyService,
                             DepartmentService departmentService,
                             OrganizationMapper mapper,
                             OrganizationEventPublisher organizationEventPublisher) {
        this.invitationRepository = invitationRepository;
        this.membershipService = membershipService;
        this.companyService = companyService;
        this.departmentService = departmentService;
        this.mapper = mapper;
        this.organizationEventPublisher = organizationEventPublisher;
    }

    /**
     * Creates an invitation and returns the response plus the single-use raw token.
     * Only the token hash is persisted; the raw value is surfaced once here because
     * email delivery is not wired yet (deferred MailHog integration).
     */
    @Transactional
    public CreatedInvitation create(UUID companyId, CurrentUser caller, InvitationRequest request) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        companyService.loadActive(companyId);

        if (request.role() == CompanyRole.OWNER) {
            throw new AppException("Ownership cannot be granted by invitation; use an explicit ownership transfer");
        }
        if (request.departmentId() != null) {
            departmentService.load(companyId, request.departmentId());
        }

        String email = request.email().trim().toLowerCase();
        invitationRepository.findByCompanyIdAndEmailAndStatus(companyId, email, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new ConflictException("A pending invitation already exists for this email");
                });

        String rawToken = generateToken();
        Invitation invitation = new Invitation(
                companyId,
                email,
                request.role(),
                request.departmentId(),
                hash(rawToken),
                caller.userId(),
                Instant.now().plus(INVITATION_TTL)
        );
        Invitation saved = invitationRepository.save(invitation);
        return new CreatedInvitation(mapper.toResponse(saved), rawToken);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> list(UUID companyId, CurrentUser caller) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        return invitationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingInvitationResponse> listPending(CurrentUser caller) {
        return invitationRepository.findByEmailAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        caller.email(), InvitationStatus.PENDING, Instant.now())
                .stream()
                .map(invitation -> new PendingInvitationResponse(
                        invitation.getId(),
                        invitation.getCompanyId(),
                        companyService.loadName(invitation.getCompanyId()),
                        invitation.getEmail(),
                        invitation.getRole(),
                        invitation.getDepartmentId(),
                        invitation.getExpiresAt(),
                        invitation.getCreatedAt()))
                .toList();
    }

    @Transactional
    public InvitationResponse revoke(UUID companyId, UUID invitationId, CurrentUser caller) {
        membershipService.requireOwnerOrAdmin(companyId, caller.userId());
        Invitation invitation = invitationRepository.findById(invitationId)
                .filter(i -> i.getCompanyId().equals(companyId))
                .orElseThrow(() -> new NotFoundException("Invitation not found"));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("Only a pending invitation can be revoked");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setRevokedAt(Instant.now());
        return mapper.toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public InvitationResponse accept(CurrentUser caller, String rawToken) {
        Invitation invitation = invitationRepository.findByToken(hash(rawToken.trim()))
                .orElseThrow(() -> new NotFoundException("Invitation not found or already used"));
        return accept(caller, invitation);
    }

    @Transactional
    public InvitationResponse accept(CurrentUser caller, UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found or already used"));
        return accept(caller, invitation);
    }

    private InvitationResponse accept(CurrentUser caller, Invitation invitation) {

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new ConflictException("This invitation has already been accepted");
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("This invitation is no longer valid");
        }
        if (invitation.isExpired()) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new ConflictException("This invitation has expired");
        }
        if (caller.email() == null || !caller.email().equals(invitation.getEmail())) {
            throw new ForbiddenException(
                    "This invitation was issued to a different account", "INVITATION_EMAIL_MISMATCH");
        }

        UUID companyId = invitation.getCompanyId();
        companyService.loadActive(companyId);
        if (membershipService.isActiveMember(companyId, caller.userId())) {
            throw new ConflictException("You are already a member of this company");
        }

        membershipService.createMembership(
                companyId,
                caller.userId(),
                caller.email(),
                invitation.getRole(),
                invitation.getDepartmentId(),
                invitation.getInvitedBy()
        );

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        return mapper.toResponse(invitationRepository.save(invitation));
    }

    @Transactional
    public int expireExpiredInvitations() {
        Instant now = Instant.now();
        List<Invitation> expired = invitationRepository.findByStatusAndExpiresAtBefore(InvitationStatus.PENDING, now);
        if (expired.isEmpty()) {
            return 0;
        }

        for (Invitation invitation : expired) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitation.setRevokedAt(now);
            organizationEventPublisher.publish(
                    "invitation.expired",
                    invitation.getCompanyId(),
                    invitation.getInvitedBy(),
                    null,
                    Map.of("email", invitation.getEmail(), "invitationId", invitation.getId())
            );
        }

        invitationRepository.saveAll(expired);
        return expired.size();
    }

    @Transactional
    public int remindPendingInvitations() {
        Instant now = Instant.now();
        Instant reminderWindowEnd = now.plus(Duration.ofHours(24));

        List<Invitation> reminders = invitationRepository.findByStatusAndExpiresAtBefore(InvitationStatus.PENDING, reminderWindowEnd)
                .stream()
                .filter(invitation -> invitation.getExpiresAt().isAfter(now))
                .toList();

        if (reminders.isEmpty()) {
            return 0;
        }

        for (Invitation invitation : reminders) {
            organizationEventPublisher.publish(
                    "invitation.reminder",
                    invitation.getCompanyId(),
                    invitation.getInvitedBy(),
                    null,
                    Map.of("email", invitation.getEmail(), "expiresAt", invitation.getExpiresAt())
            );
        }

        return reminders.size();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public record CreatedInvitation(InvitationResponse invitation, String token) {
    }
}
