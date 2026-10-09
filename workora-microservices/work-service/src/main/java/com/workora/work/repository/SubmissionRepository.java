package com.workora.work.repository;

import com.workora.work.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByTaskIdOrderBySubmittedAtAsc(UUID taskId);

    long countByTaskId(UUID taskId);
}
