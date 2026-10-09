package com.workora.payment.repository;

import com.workora.payment.entity.PaymentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentProfileRepository extends JpaRepository<PaymentProfile, UUID> {
    Optional<PaymentProfile> findByOwnerIdAndProvider(UUID ownerId, String provider);
}
