package com.workora.organization.repository;

import com.workora.organization.entity.CompanyStatus;
import com.workora.organization.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findByCompanyIdAndStatusOrderByCreatedAtAsc(UUID companyId, CompanyStatus status);

    List<Department> findByCompanyIdAndStatusAndNameContainingIgnoreCase(UUID companyId, CompanyStatus status, String name);

    Optional<Department> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameAndStatus(UUID companyId, String name, CompanyStatus status);

    long countByCompanyIdAndStatus(UUID companyId, CompanyStatus status);
}
