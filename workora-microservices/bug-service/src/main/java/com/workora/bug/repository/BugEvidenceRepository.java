package com.workora.bug.repository;

import com.workora.bug.entity.BugEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugEvidenceRepository extends JpaRepository<BugEvidence, UUID> {

    List<BugEvidence> findByReportId(UUID reportId);
}
