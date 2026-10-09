package com.workora.organization.repository;

import com.workora.organization.entity.MembershipAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface MembershipAuditRepository extends JpaRepository<MembershipAudit, UUID> {

    List<MembershipAudit> findByCompanyIdAndUserIdOrderByCreatedAtDesc(UUID companyId, UUID userId);

    List<MembershipAudit> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    @Query("""
            select audit from MembershipAudit audit
            where (:companyId is null or audit.companyId = :companyId)
              and (:userId is null or audit.userId = :userId)
              and (:action is null or audit.action = :action)
              and (:startTime is null or audit.createdAt >= :startTime)
              and (:endTime is null or audit.createdAt < :endTime)
            """)
    Page<MembershipAudit> searchAdminActivity(
            @Param("companyId") UUID companyId,
            @Param("userId") UUID userId,
            @Param("action") String action,
            @Param("startTime") Instant from,
            @Param("endTime") Instant to,
            Pageable pageable);
}
