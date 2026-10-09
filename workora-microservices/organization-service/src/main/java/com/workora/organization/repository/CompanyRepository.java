package com.workora.organization.repository;

import com.workora.organization.entity.Company;
import com.workora.organization.entity.CompanyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByIdAndStatus(UUID id, CompanyStatus status);

    Page<Company> findByOwnerUserIdAndStatus(UUID ownerUserId, CompanyStatus status, Pageable pageable);

    Page<Company> findByOwnerUserIdAndStatusAndNameContainingIgnoreCase(UUID ownerUserId, CompanyStatus status, String name, Pageable pageable);

    Page<Company> findByStatusAndNameContainingIgnoreCase(CompanyStatus status, String name, Pageable pageable);

    boolean existsByNameAndStatus(String name, CompanyStatus status);

    long countByOwnerUserIdAndStatus(UUID ownerUserId, CompanyStatus status);
}
