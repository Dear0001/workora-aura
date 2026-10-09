package com.workora.payment.repository;

import com.workora.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByPayerIdAndReference(UUID payerId, String reference);
    Optional<Payment> findByReference(String reference);
    Page<Payment> findByPayerIdOrderByCreatedAtDesc(UUID payerId, Pageable pageable);
}
