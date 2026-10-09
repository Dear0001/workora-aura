package com.workora.project.repository;

import com.workora.project.entity.BugFixJobStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BugFixJobStatusHistoryRepository extends JpaRepository<BugFixJobStatusHistory, UUID> {

    List<BugFixJobStatusHistory> findByJobIdOrderByChangedAtAsc(UUID jobId);
}
