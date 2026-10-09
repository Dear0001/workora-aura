package com.workora.project.repository;

import com.workora.project.entity.PhaseMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhaseMemberRepository extends JpaRepository<PhaseMember, UUID> {
    @Query("select m from PhaseMember m where m.phaseId in (select p.id from Phase p where p.projectId = :projectId) order by m.createdAt asc")
    List<PhaseMember> findByProjectId(@Param("projectId") UUID projectId);

    List<PhaseMember> findByPhaseIdOrderByCreatedAtAsc(UUID phaseId);

    Optional<PhaseMember> findByPhaseIdAndUserId(UUID phaseId, UUID userId);

    boolean existsByPhaseIdAndUserId(UUID phaseId, UUID userId);

    void deleteByPhaseIdAndUserId(UUID phaseId, UUID userId);
}
