package com.workora.identity.repository;

import com.workora.identity.entity.AdminAccessRequest;
import com.workora.identity.entity.AdminAccessRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AdminAccessRequestRepository extends JpaRepository<AdminAccessRequest, UUID> {
    List<AdminAccessRequest> findByStatusOrderByCreatedAtDesc(AdminAccessRequestStatus status);
    boolean existsByEmailAndStatus(String email, AdminAccessRequestStatus status);
}
