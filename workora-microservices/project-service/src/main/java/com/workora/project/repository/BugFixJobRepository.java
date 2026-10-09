package com.workora.project.repository;

import com.workora.project.entity.BugFixJob;
import com.workora.project.entity.BugFixJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugFixJobRepository extends JpaRepository<BugFixJob, UUID> {

    List<BugFixJob> findByPhaseIdOrderByCreatedAtDesc(UUID phaseId);

    Page<BugFixJob> findByPhaseIdAndTitleContainingIgnoreCase(UUID phaseId, String title, Pageable pageable);

    Page<BugFixJob> findByPhaseIdAndStatusAndTitleContainingIgnoreCase(UUID phaseId, BugFixJobStatus status, String title, Pageable pageable);

    List<BugFixJob> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    Page<BugFixJob> findByCompanyIdAndStatus(UUID companyId, BugFixJobStatus status, Pageable pageable);

    Page<BugFixJob> findByCompanyIdAndTitleContainingIgnoreCase(UUID companyId, String title, Pageable pageable);

    Page<BugFixJob> findByCompanyIdAndStatusAndTitleContainingIgnoreCase(UUID companyId, BugFixJobStatus status, String title, Pageable pageable);

    Page<BugFixJob> findByStatus(BugFixJobStatus status, Pageable pageable);

    List<BugFixJob> findBySelectedFreelancerId(UUID selectedFreelancerId);

    List<BugFixJob> findByProjectManagerId(UUID projectManagerId);
}
