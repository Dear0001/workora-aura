package com.workora.project.repository;

import com.workora.project.entity.BugFixJobSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugFixJobSubmissionRepository extends JpaRepository<BugFixJobSubmission, UUID> {

    List<BugFixJobSubmission> findByJobIdOrderBySubmittedAtAsc(UUID jobId);

    long countByJobId(UUID jobId);
}
