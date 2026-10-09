package com.workora.organization.repository;

import com.workora.organization.entity.CompanyRole;
import com.workora.organization.entity.Membership;
import com.workora.organization.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByCompanyIdAndUserIdAndStatus(UUID companyId, UUID userId, MembershipStatus status);

    List<Membership> findByCompanyIdAndStatusOrderByCreatedAtAsc(UUID companyId, MembershipStatus status);

    List<Membership> findByCompanyIdAndStatusAndEmailContainingIgnoreCase(UUID companyId, MembershipStatus status, String email);

    List<Membership> findByCompanyIdAndStatusAndRoleNotOrderByCreatedAtAsc(
            UUID companyId, MembershipStatus status, CompanyRole role);

    List<Membership> findByCompanyIdAndStatusAndRoleNotAndEmailContainingIgnoreCase(
            UUID companyId, MembershipStatus status, CompanyRole role, String email);

    List<Membership> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, MembershipStatus status);

    List<Membership> findByDepartmentIdAndStatus(UUID departmentId, MembershipStatus status);

    long countByCompanyIdAndRoleAndStatus(UUID companyId, CompanyRole role, MembershipStatus status);

    boolean existsByCompanyIdAndUserIdAndStatus(UUID companyId, UUID userId, MembershipStatus status);
}
