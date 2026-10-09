package com.workora.project.repository;

import com.workora.project.entity.BugFixJobReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugFixJobReviewRepository extends JpaRepository<BugFixJobReview, UUID> {

    List<BugFixJobReview> findByJobIdOrderByCreatedAtAsc(UUID jobId);
}
