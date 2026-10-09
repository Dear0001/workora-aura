package com.workora.organization.repository;

import com.workora.organization.entity.Invitation;
import com.workora.organization.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    Optional<Invitation> findByToken(String token);

    List<Invitation> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Optional<Invitation> findByCompanyIdAndEmailAndStatus(UUID companyId, String email, InvitationStatus status);

    List<Invitation> findByEmailAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            String email, InvitationStatus status, Instant expiresAt);

    List<Invitation> findByStatusAndExpiresAtBefore(InvitationStatus status, Instant expiresAt);
}
