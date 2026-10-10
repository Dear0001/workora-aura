package com.workora.organization.repository;

import com.workora.organization.entity.DepartmentMembership;
import com.workora.organization.entity.DepartmentRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembership, UUID> {
    List<DepartmentMembership> findByDepartmentId(UUID departmentId);

    List<DepartmentMembership> findByMembership_IdIn(List<UUID> membershipIds);

    Optional<DepartmentMembership> findByMembership_IdAndDepartmentId(UUID membershipId, UUID departmentId);

    boolean existsByMembership_IdAndDepartmentIdAndRole(UUID membershipId, UUID departmentId, DepartmentRole role);

    boolean existsByMembership_IdAndDepartmentId(UUID membershipId, UUID departmentId);
}
