package com.workora.organization.repository;

import com.workora.organization.entity.CompanyRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRatingRepository extends JpaRepository<CompanyRating, UUID> {

    Page<CompanyRating> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    Optional<CompanyRating> findByCompanyIdAndUserId(UUID companyId, UUID userId);

    long countByCompanyId(UUID companyId);
}
