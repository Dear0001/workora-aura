package com.workora.identity.repository;

import com.workora.identity.entity.RegistrationVerificationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationVerificationSessionRepository
        extends JpaRepository<RegistrationVerificationSession, UUID> {
    Optional<RegistrationVerificationSession> findByTokenHash(String tokenHash);
}
