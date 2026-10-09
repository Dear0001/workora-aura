package com.workora.project.repository;

import com.workora.project.entity.Phase;
import com.workora.project.entity.PhaseStatus;
import com.workora.project.entity.PhaseVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhaseRepository extends JpaRepository<Phase, UUID> {

    List<Phase> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Page<Phase> findByProjectIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(UUID projectId, String name, Pageable pageable);

    Page<Phase> findByProjectIdAndStatusOrderByCreatedAtDesc(UUID projectId, PhaseStatus status, Pageable pageable);

    Page<Phase> findByProjectIdAndStatusAndNameContainingIgnoreCaseOrderByCreatedAtDesc(UUID projectId, PhaseStatus status, String name, Pageable pageable);

    Optional<Phase> findByIdAndProjectId(UUID id, UUID projectId);

    Page<Phase> findByStatusAndVisibility(PhaseStatus status, PhaseVisibility visibility, Pageable pageable);

    long countByProjectIdAndStatus(UUID projectId, PhaseStatus status);
}
