package com.workora.organization.repository;

import com.workora.organization.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUserId(UUID userId);

    Page<Subscription> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
