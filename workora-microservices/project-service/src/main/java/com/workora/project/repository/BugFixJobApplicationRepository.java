package com.workora.project.repository;

import com.workora.project.entity.BugFixJobApplication;
import com.workora.project.entity.JobApplicationDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BugFixJobApplicationRepository extends JpaRepository<BugFixJobApplication, UUID> {

    List<BugFixJobApplication> findByJobIdOrderByCreatedAtAsc(UUID jobId);

    Optional<BugFixJobApplication> findByJobIdAndApplicantId(UUID jobId, UUID applicantId);

    long countByJobIdAndDecision(UUID jobId, JobApplicationDecision decision);

    List<BugFixJobApplication> findByApplicantId(UUID applicantId);
}
